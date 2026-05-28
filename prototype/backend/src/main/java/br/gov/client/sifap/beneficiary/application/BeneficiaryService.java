package br.gov.client.sifap.beneficiary.application;

import br.gov.client.sifap.audit.application.AuditService;
import br.gov.client.sifap.beneficiary.infrastructure.persistence.BeneficiaryEntity;
import br.gov.client.sifap.beneficiary.infrastructure.persistence.BeneficiaryRepository;
import br.gov.client.sifap.beneficiary.infrastructure.persistence.DependentEntity;
import br.gov.client.sifap.catalog.infrastructure.persistence.SocialProgramEntity;
import br.gov.client.sifap.catalog.infrastructure.persistence.SocialProgramRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class BeneficiaryService {

    private final BeneficiaryRepository repository;
    private final SocialProgramRepository programRepository;
    private final AuditService auditService;

    public BeneficiaryService(BeneficiaryRepository repository, SocialProgramRepository programRepository, AuditService auditService) {
        this.repository = repository;
        this.programRepository = programRepository;
        this.auditService = auditService;
    }

    @Transactional
    public BeneficiaryEntity create(CreateBeneficiaryCommand command) {
        if (repository.existsByCpf(command.cpf())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado");
        }
        if (command.dependents().size() > 10) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "limite de 10 dependentes atingido");
        }

        SocialProgramEntity program = programRepository.findById(command.programCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Programa não encontrado"));

        BeneficiaryEntity entity = new BeneficiaryEntity();
        entity.setCpf(command.cpf());
        entity.setName(command.name());
        entity.setBirthDate(command.birthDate());
        entity.setStatus(command.status());
        entity.setUf(command.uf());
        entity.setRegionCode(command.regionCode());
        entity.setFamilyIncome(command.familyIncome());
        entity.setProgram(program);

        command.dependents().forEach(item -> {
            DependentEntity dependent = new DependentEntity();
            dependent.setBeneficiary(entity);
            dependent.setCpf(item.cpf());
            dependent.setName(item.name());
            dependent.setBirthDate(item.birthDate());
            dependent.setRelationshipCode(item.relationshipCode());
            dependent.setStatus(item.status());
            dependent.setDisabilityIndicator(item.disabilityIndicator());
            entity.getDependents().add(dependent);
        });

        BeneficiaryEntity saved = repository.save(entity);
        auditService.log("IN", "beneficiary", saved.getCpf(), "Beneficiário cadastrado");
        return saved;
    }

    @Transactional(readOnly = true)
    public BeneficiaryEntity getByCpf(String cpf) {
        return repository.findByCpf(cpf)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Beneficiário não encontrado"));
    }

    @Transactional
    public BeneficiaryEntity registerBiometrics(String cpf, RegisterBiometricsCommand command) {
        BeneficiaryEntity entity = getByCpf(cpf);
        entity.setBiometricsStatus("S");
        entity.setBiometricsCollectedAt(LocalDate.now());
        entity.setBiometricsPostCode(command.postCode());
        entity.setBiometricsHash(command.hash());
        auditService.log("AL", "beneficiary", cpf, "Biometria registrada");
        return entity;
    }

    public record CreateBeneficiaryCommand(
            String cpf,
            String name,
            LocalDate birthDate,
            String status,
            String uf,
            String regionCode,
            java.math.BigDecimal familyIncome,
            String programCode,
            List<DependentCommand> dependents) {
    }

    public record DependentCommand(
            String cpf,
            String name,
            LocalDate birthDate,
            String relationshipCode,
            String status,
            String disabilityIndicator) {
    }

    public record RegisterBiometricsCommand(String hash, String postCode) {
    }
}