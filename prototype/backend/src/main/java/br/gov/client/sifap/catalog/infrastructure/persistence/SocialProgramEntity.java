package br.gov.client.sifap.catalog.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "social_program", schema = "catalog")
public class SocialProgramEntity {

    @Id
    @Column(length = 4)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "program_type", nullable = false, length = 1)
    private String programType;

    @Column(name = "base_value_original", nullable = false, precision = 11, scale = 2)
    private BigDecimal baseValueOriginal;

    @Column(name = "base_value_adjusted", nullable = false, precision = 11, scale = 2)
    private BigDecimal baseValueAdjusted;

    @Column(name = "factor_reajuste", nullable = false, precision = 9, scale = 4)
    private BigDecimal factorReajuste;

    @Column(name = "factor_k", nullable = false, precision = 10, scale = 7)
    private BigDecimal factorK;

    @Column(name = "biometrics_required", nullable = false)
    private boolean biometricsRequired;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<RegionalParameterEntity> regionalParameters = new LinkedHashSet<>();

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProgramType() { return programType; }
    public void setProgramType(String programType) { this.programType = programType; }
    public BigDecimal getBaseValueOriginal() { return baseValueOriginal; }
    public void setBaseValueOriginal(BigDecimal baseValueOriginal) { this.baseValueOriginal = baseValueOriginal; }
    public BigDecimal getBaseValueAdjusted() { return baseValueAdjusted; }
    public void setBaseValueAdjusted(BigDecimal baseValueAdjusted) { this.baseValueAdjusted = baseValueAdjusted; }
    public BigDecimal getFactorReajuste() { return factorReajuste; }
    public void setFactorReajuste(BigDecimal factorReajuste) { this.factorReajuste = factorReajuste; }
    public BigDecimal getFactorK() { return factorK; }
    public void setFactorK(BigDecimal factorK) { this.factorK = factorK; }
    public boolean isBiometricsRequired() { return biometricsRequired; }
    public void setBiometricsRequired(boolean biometricsRequired) { this.biometricsRequired = biometricsRequired; }
    public Set<RegionalParameterEntity> getRegionalParameters() { return regionalParameters; }
}