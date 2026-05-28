package br.gov.client.sifap.payment.infrastructure.api;

import br.gov.client.sifap.payment.application.PaymentCalculationService;
import br.gov.client.sifap.payment.infrastructure.persistence.PaymentDiscountEntity;
import br.gov.client.sifap.payment.infrastructure.persistence.PaymentEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentCalculationService service;

    public PaymentController(PaymentCalculationService service) {
        this.service = service;
    }

    @PostMapping("/pagamentos:calcular")
    public PaymentResponse calculate(@Valid @RequestBody CalculatePaymentRequest request) {
        PaymentEntity payment = service.calculate(new PaymentCalculationService.CalculatePaymentCommand(
                request.cpf(),
                request.competencia(),
                request.descontos().stream()
                        .map(item -> new PaymentCalculationService.DiscountCommand(
                                item.tipo(), item.valor(), item.dtInicio(), item.dtFim()))
                        .toList()));
        return PaymentResponse.from(payment);
    }

    public record CalculatePaymentRequest(
            @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
            @NotBlank @Pattern(regexp = "\\d{6}") String competencia,
            @NotNull List<@Valid DiscountRequest> descontos) {
    }

    public record DiscountRequest(
            @NotBlank @Pattern(regexp = "IR|JD|CS|PA|EM|TX|OU|EX") String tipo,
            @NotNull @DecimalMin("0.00") BigDecimal valor,
            LocalDate dtInicio,
            LocalDate dtFim) {
    }

    public record PaymentResponse(
            Long id,
            String cpf,
            String competencia,
            BigDecimal valorBruto,
            BigDecimal valorDescontos,
            BigDecimal valorLiquido,
            BigDecimal valorDecimoTerceiro,
            BigDecimal valorAbono,
            String tipoPagamento,
            String situacao,
            List<DiscountResponse> descontos) {

        static PaymentResponse from(PaymentEntity entity) {
            return new PaymentResponse(
                    entity.getId(),
                    entity.getBeneficiaryCpf(),
                    entity.getCompetence(),
                    entity.getGrossAmount(),
                    entity.getDeductionTotal(),
                    entity.getNetAmount(),
                    entity.getThirteenthAmount(),
                    entity.getChristmasBonus(),
                    entity.getPaymentType(),
                    entity.getStatus(),
                    entity.getDiscounts().stream().map(item -> new DiscountResponse(item.getDiscountType(), item.getAmount())).toList());
        }
    }

    public record DiscountResponse(String tipo, BigDecimal valor) {
        static DiscountResponse from(PaymentDiscountEntity entity) {
            return new DiscountResponse(entity.getDiscountType(), entity.getAmount());
        }
    }
}