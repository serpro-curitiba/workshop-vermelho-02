package br.gov.client.sifap.payment.infrastructure.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_record", schema = "payment")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "beneficiary_cpf", nullable = false, length = 11)
    private String beneficiaryCpf;

    @Column(nullable = false, length = 6)
    private String competence;

    @Column(name = "gross_amount", nullable = false, precision = 11, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "deduction_total", nullable = false, precision = 11, scale = 2)
    private BigDecimal deductionTotal;

    @Column(name = "net_amount", nullable = false, precision = 11, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "thirteenth_amount", nullable = false, precision = 11, scale = 2)
    private BigDecimal thirteenthAmount;

    @Column(name = "christmas_bonus", nullable = false, precision = 11, scale = 2)
    private BigDecimal christmasBonus;

    @Column(name = "payment_type", nullable = false, length = 1)
    private String paymentType;

    @Column(nullable = false, length = 1)
    private String status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PaymentDiscountEntity> discounts = new ArrayList<>();

    public Long getId() { return id; }
    public String getBeneficiaryCpf() { return beneficiaryCpf; }
    public void setBeneficiaryCpf(String beneficiaryCpf) { this.beneficiaryCpf = beneficiaryCpf; }
    public String getCompetence() { return competence; }
    public void setCompetence(String competence) { this.competence = competence; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getDeductionTotal() { return deductionTotal; }
    public void setDeductionTotal(BigDecimal deductionTotal) { this.deductionTotal = deductionTotal; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public BigDecimal getThirteenthAmount() { return thirteenthAmount; }
    public void setThirteenthAmount(BigDecimal thirteenthAmount) { this.thirteenthAmount = thirteenthAmount; }
    public BigDecimal getChristmasBonus() { return christmasBonus; }
    public void setChristmasBonus(BigDecimal christmasBonus) { this.christmasBonus = christmasBonus; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<PaymentDiscountEntity> getDiscounts() { return discounts; }
}