package br.gov.client.sifap.payment.application;

import br.gov.client.sifap.audit.application.AuditService;
import br.gov.client.sifap.beneficiary.infrastructure.persistence.BeneficiaryEntity;
import br.gov.client.sifap.beneficiary.infrastructure.persistence.BeneficiaryRepository;
import br.gov.client.sifap.beneficiary.infrastructure.persistence.DependentEntity;
import br.gov.client.sifap.catalog.infrastructure.persistence.RegionalParameterEntity;
import br.gov.client.sifap.catalog.infrastructure.persistence.SocialProgramEntity;
import br.gov.client.sifap.payment.infrastructure.persistence.PaymentDiscountEntity;
import br.gov.client.sifap.payment.infrastructure.persistence.PaymentEntity;
import br.gov.client.sifap.payment.infrastructure.persistence.PaymentRepository;
import br.gov.client.sifap.shared.application.MoneyUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PaymentCalculationService {

    private final PaymentRepository paymentRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final AuditService auditService;

    public PaymentCalculationService(PaymentRepository paymentRepository,
                                     BeneficiaryRepository beneficiaryRepository,
                                     AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.auditService = auditService;
    }

    @Transactional
    public PaymentEntity calculate(CalculatePaymentCommand command) {
        PaymentEntity existing = paymentRepository.findByBeneficiaryCpfAndCompetence(command.cpf(), command.competence()).orElse(null);
        if (existing != null) {
            return existing;
        }

        BeneficiaryEntity beneficiary = beneficiaryRepository.findByCpf(command.cpf())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiário não encontrado"));

        if (!"A".equals(beneficiary.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "BENEFICIARIO NAO ATIVO");
        }

        SocialProgramEntity program = beneficiary.getProgram();
        if (program.isBiometricsRequired() && !"S".equals(beneficiary.getBiometricsStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "BIOMETRIA PENDENTE");
        }

        BigDecimal regionalFactor = regionalFactor(program, beneficiary.getUf());
        BigDecimal familyFactor = familyFactor(beneficiary.getDependents());
        BigDecimal incomeFactor = incomeFactor(beneficiary.getFamilyIncome());
        BigDecimal ageFactor = ageFactor(beneficiary.getBirthDate(), command.competence());

        BigDecimal monthlyAmount = MoneyUtils.truncate(program.getBaseValueAdjusted()
                .multiply(regionalFactor)
                .multiply(familyFactor)
                .multiply(incomeFactor)
                .multiply(ageFactor));

        BigDecimal thirteenthAmount = BigDecimal.ZERO;
        BigDecimal christmasBonus = BigDecimal.ZERO;
        String paymentType = "N";

        YearMonth competence = YearMonth.of(Integer.parseInt(command.competence().substring(0, 4)),
                Integer.parseInt(command.competence().substring(4, 6)));
        if (competence.getMonthValue() == 12) {
            thirteenthAmount = MoneyUtils.truncate(program.getBaseValueAdjusted().multiply(regionalFactor).multiply(ageFactor));
            paymentType = "D";
            if ("A".equals(program.getProgramType())) {
                christmasBonus = MoneyUtils.truncate(monthlyAmount.multiply(new BigDecimal("0.15")));
            }
        }

        BigDecimal grossAmount = MoneyUtils.truncate(monthlyAmount.add(thirteenthAmount).add(christmasBonus));
        List<PaymentDiscountEntity> discounts = buildDiscounts(command, grossAmount);
        BigDecimal deductionTotal = discounts.stream().map(PaymentDiscountEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        deductionTotal = MoneyUtils.truncate(deductionTotal);
        BigDecimal netAmount = MoneyUtils.truncate(grossAmount.subtract(deductionTotal));

        PaymentEntity entity = new PaymentEntity();
        entity.setBeneficiaryCpf(command.cpf());
        entity.setCompetence(command.competence());
        entity.setGrossAmount(grossAmount);
        entity.setThirteenthAmount(thirteenthAmount);
        entity.setChristmasBonus(christmasBonus);
        entity.setDeductionTotal(deductionTotal);
        entity.setNetAmount(netAmount);
        entity.setPaymentType(paymentType);
        entity.setStatus("G");

        discounts.forEach(discount -> {
            discount.setPayment(entity);
            entity.getDiscounts().add(discount);
        });

        PaymentEntity saved = paymentRepository.save(entity);
        auditService.log("IN", "payment", saved.getId().toString(), "Pagamento calculado");
        return saved;
    }

    private List<PaymentDiscountEntity> buildDiscounts(CalculatePaymentCommand command, BigDecimal grossAmount) {
        List<PaymentDiscountEntity> validDiscounts = new ArrayList<>();

        PaymentDiscountEntity socialContribution = new PaymentDiscountEntity();
        socialContribution.setDiscountType("SOC");
        socialContribution.setAmount(MoneyUtils.truncate(grossAmount.multiply(socialRate(grossAmount))));
        validDiscounts.add(socialContribution);

        LocalDate processingDate = LocalDate.now();
        BigDecimal judicialTotal = BigDecimal.ZERO;
        BigDecimal nonJudicialTotal = socialContribution.getAmount();

        for (DiscountCommand item : command.discounts()) {
            if (item.startDate() != null && item.startDate().isAfter(processingDate)) {
                continue;
            }
            if (item.endDate() != null && item.endDate().isBefore(processingDate)) {
                continue;
            }

            PaymentDiscountEntity entity = new PaymentDiscountEntity();
            entity.setDiscountType(item.type());
            entity.setAmount(MoneyUtils.truncate(item.amount()));
            entity.setStartDate(item.startDate());
            entity.setEndDate(item.endDate());

            if ("JD".equals(item.type())) {
                judicialTotal = judicialTotal.add(entity.getAmount());
            } else {
                nonJudicialTotal = nonJudicialTotal.add(entity.getAmount());
            }
            validDiscounts.add(entity);
        }

        BigDecimal nonJudicialCap = MoneyUtils.truncate(grossAmount.multiply(new BigDecimal("0.30")));
        BigDecimal cappedNonJudicial = nonJudicialTotal.min(nonJudicialCap);

        List<PaymentDiscountEntity> normalized = new ArrayList<>();
        PaymentDiscountEntity cappedSocial = validDiscounts.getFirst();
        cappedSocial.setAmount(cappedNonJudicial.min(cappedSocial.getAmount()));
        normalized.add(cappedSocial);

        BigDecimal remainingNonJudicial = cappedNonJudicial.subtract(cappedSocial.getAmount());
        for (PaymentDiscountEntity item : validDiscounts.stream().skip(1).sorted(Comparator.comparing(PaymentDiscountEntity::getDiscountType)).toList()) {
            if ("JD".equals(item.getDiscountType())) {
                normalized.add(item);
            } else if (remainingNonJudicial.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal applied = item.getAmount().min(remainingNonJudicial);
                item.setAmount(applied);
                normalized.add(item);
                remainingNonJudicial = remainingNonJudicial.subtract(applied);
            }
        }

        return normalized;
    }

    private BigDecimal socialRate(BigDecimal grossAmount) {
        if (grossAmount.compareTo(new BigDecimal("500.00")) <= 0) {
            return new BigDecimal("0.03");
        }
        if (grossAmount.compareTo(new BigDecimal("1000.00")) <= 0) {
            return new BigDecimal("0.05");
        }
        if (grossAmount.compareTo(new BigDecimal("2000.00")) <= 0) {
            return new BigDecimal("0.07");
        }
        return new BigDecimal("0.09");
    }

    private BigDecimal regionalFactor(SocialProgramEntity program, String uf) {
        return program.getRegionalParameters().stream()
                .filter(item -> uf.equals(item.getUf()))
                .map(RegionalParameterEntity::getFactorRegional)
                .findFirst()
                .orElse(new BigDecimal("1.0000"));
    }

    private BigDecimal familyFactor(List<DependentEntity> dependents) {
        long activeDependents = dependents.stream().filter(item -> "A".equals(item.getStatus())).count();
        if (activeDependents == 0) {
            return new BigDecimal("1.0000");
        }
        if (activeDependents <= 2) {
            return new BigDecimal("1.0000").add(new BigDecimal("0.0500").multiply(BigDecimal.valueOf(activeDependents)));
        }
        if (activeDependents <= 4) {
            return new BigDecimal("1.1000").add(new BigDecimal("0.0300").multiply(BigDecimal.valueOf(activeDependents - 2)));
        }
        return new BigDecimal("1.1600").add(new BigDecimal("0.0200").multiply(BigDecimal.valueOf(activeDependents - 4)));
    }

    private BigDecimal incomeFactor(BigDecimal familyIncome) {
        if (familyIncome.compareTo(new BigDecimal("300.00")) <= 0) {
            return new BigDecimal("1.0000");
        }
        if (familyIncome.compareTo(new BigDecimal("600.00")) <= 0) {
            return new BigDecimal("0.8500");
        }
        if (familyIncome.compareTo(new BigDecimal("1000.00")) <= 0) {
            return new BigDecimal("0.7000");
        }
        if (familyIncome.compareTo(new BigDecimal("1500.00")) <= 0) {
            return new BigDecimal("0.5500");
        }
        return new BigDecimal("0.4000");
    }

    private BigDecimal ageFactor(LocalDate birthDate, String competence) {
        int age = Integer.parseInt(competence.substring(0, 4)) - birthDate.getYear();
        if (age >= 65) {
            return new BigDecimal("1.1500");
        }
        if (age >= 60) {
            return new BigDecimal("1.1000");
        }
        if (age < 18) {
            return new BigDecimal("1.0500");
        }
        return new BigDecimal("1.0000");
    }

    public record CalculatePaymentCommand(String cpf, String competence, List<DiscountCommand> discounts) {
    }

    public record DiscountCommand(String type, BigDecimal amount, LocalDate startDate, LocalDate endDate) {
    }
}