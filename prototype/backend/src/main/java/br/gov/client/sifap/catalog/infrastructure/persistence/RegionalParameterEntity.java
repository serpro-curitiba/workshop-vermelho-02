package br.gov.client.sifap.catalog.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "regional_parameter", schema = "catalog")
public class RegionalParameterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_code", nullable = false)
    private SocialProgramEntity program;

    @Column(nullable = false, length = 2)
    private String uf;

    @Column(name = "factor_regional", nullable = false, precision = 9, scale = 4)
    private BigDecimal factorRegional;

    public Long getId() { return id; }
    public SocialProgramEntity getProgram() { return program; }
    public void setProgram(SocialProgramEntity program) { this.program = program; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
    public BigDecimal getFactorRegional() { return factorRegional; }
    public void setFactorRegional(BigDecimal factorRegional) { this.factorRegional = factorRegional; }
}