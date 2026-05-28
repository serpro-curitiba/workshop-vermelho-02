package br.gov.client.sifap.beneficiary.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "dependent", schema = "beneficiary")
public class DependentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private BeneficiaryEntity beneficiary;

    @Column(length = 11)
    private String cpf;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "relationship_code", nullable = false, length = 2)
    private String relationshipCode;

    @Column(nullable = false, length = 1)
    private String status;

    @Column(name = "disability_indicator", nullable = false, length = 1)
    private String disabilityIndicator;

    public Long getId() { return id; }
    public BeneficiaryEntity getBeneficiary() { return beneficiary; }
    public void setBeneficiary(BeneficiaryEntity beneficiary) { this.beneficiary = beneficiary; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getRelationshipCode() { return relationshipCode; }
    public void setRelationshipCode(String relationshipCode) { this.relationshipCode = relationshipCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDisabilityIndicator() { return disabilityIndicator; }
    public void setDisabilityIndicator(String disabilityIndicator) { this.disabilityIndicator = disabilityIndicator; }
}