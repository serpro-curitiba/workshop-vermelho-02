package br.gov.client.sifap.beneficiary.infrastructure.persistence;

import br.gov.client.sifap.catalog.infrastructure.persistence.SocialProgramEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "beneficiary", schema = "beneficiary")
public class BeneficiaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 1)
    private String status;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(name = "region_code", nullable = false, length = 2)
    private String regionCode;

    @Column(name = "family_income", nullable = false, precision = 11, scale = 2)
    private BigDecimal familyIncome;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "program_code", nullable = false)
    private SocialProgramEntity program;

    @Column(name = "biometrics_status", nullable = false, length = 1)
    private String biometricsStatus = "N";

    @Column(name = "biometrics_collected_at")
    private LocalDate biometricsCollectedAt;

    @Column(name = "biometrics_post_code", length = 6)
    private String biometricsPostCode;

    @Column(name = "biometrics_hash", length = 64)
    private String biometricsHash;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "beneficiary", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DependentEntity> dependents = new ArrayList<>();

    public Long getId() { return id; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
    public BigDecimal getFamilyIncome() { return familyIncome; }
    public void setFamilyIncome(BigDecimal familyIncome) { this.familyIncome = familyIncome; }
    public SocialProgramEntity getProgram() { return program; }
    public void setProgram(SocialProgramEntity program) { this.program = program; }
    public String getBiometricsStatus() { return biometricsStatus; }
    public void setBiometricsStatus(String biometricsStatus) { this.biometricsStatus = biometricsStatus; }
    public LocalDate getBiometricsCollectedAt() { return biometricsCollectedAt; }
    public void setBiometricsCollectedAt(LocalDate biometricsCollectedAt) { this.biometricsCollectedAt = biometricsCollectedAt; }
    public String getBiometricsPostCode() { return biometricsPostCode; }
    public void setBiometricsPostCode(String biometricsPostCode) { this.biometricsPostCode = biometricsPostCode; }
    public String getBiometricsHash() { return biometricsHash; }
    public void setBiometricsHash(String biometricsHash) { this.biometricsHash = biometricsHash; }
    public List<DependentEntity> getDependents() { return dependents; }
}