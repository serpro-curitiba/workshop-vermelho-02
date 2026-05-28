package br.gov.client.sifap.catalog.application;

import br.gov.client.sifap.audit.application.AuditService;
import br.gov.client.sifap.catalog.infrastructure.persistence.RegionalParameterEntity;
import br.gov.client.sifap.catalog.infrastructure.persistence.SocialProgramEntity;
import br.gov.client.sifap.catalog.infrastructure.persistence.SocialProgramRepository;
import br.gov.client.sifap.shared.application.MoneyUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CatalogService {

    private static final BigDecimal FATOR_K_MULTIPLIER = new BigDecimal("0.347215");

    private final SocialProgramRepository repository;
    private final AuditService auditService;

    public CatalogService(SocialProgramRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public SocialProgramEntity createProgram(CreateProgramCommand command) {
        if (repository.existsById(command.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Programa já cadastrado");
        }

        BigDecimal factorK = BigDecimal.ONE.add(command.factorReajuste().multiply(FATOR_K_MULTIPLIER));
        BigDecimal adjustedBase = MoneyUtils.truncate(command.baseValue().multiply(factorK));

        SocialProgramEntity entity = new SocialProgramEntity();
        entity.setCode(command.code());
        entity.setName(command.name());
        entity.setProgramType(command.programType());
        entity.setBaseValueOriginal(MoneyUtils.truncate(command.baseValue()));
        entity.setBaseValueAdjusted(adjustedBase);
        entity.setFactorReajuste(command.factorReajuste());
        entity.setFactorK(factorK);
        entity.setBiometricsRequired(command.biometricsRequired());

        command.regionalFactors().forEach(factor -> {
            RegionalParameterEntity regional = new RegionalParameterEntity();
            regional.setProgram(entity);
            regional.setUf(factor.uf());
            regional.setFactorRegional(factor.factorRegional());
            entity.getRegionalParameters().add(regional);
        });

        SocialProgramEntity saved = repository.save(entity);
        auditService.log("IN", "program", saved.getCode(), "Programa social cadastrado");
        return saved;
    }

    @Transactional(readOnly = true)
    public SocialProgramEntity getProgram(String code) {
        return repository.findById(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programa não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<SocialProgramEntity> listPrograms() {
        return repository.findAllByOrderByCodeAsc();
    }

    public record CreateProgramCommand(
            String code,
            String name,
            String programType,
            BigDecimal baseValue,
            BigDecimal factorReajuste,
            boolean biometricsRequired,
            List<RegionalFactorCommand> regionalFactors) {
    }

    public record RegionalFactorCommand(String uf, BigDecimal factorRegional) {
    }
}