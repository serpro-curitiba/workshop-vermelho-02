package br.gov.client.sifap.beneficiary.infrastructure.api;

import br.gov.client.sifap.beneficiary.application.BeneficiaryService;
import br.gov.client.sifap.beneficiary.infrastructure.persistence.BeneficiaryEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/beneficiarios")
public class BeneficiaryController {

    private final BeneficiaryService service;

    public BeneficiaryController(BeneficiaryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryResponse create(@Valid @RequestBody CreateBeneficiaryRequest request) {
        BeneficiaryEntity saved = service.create(new BeneficiaryService.CreateBeneficiaryCommand(
                request.cpf(),
                request.nome(),
                request.dataNascimento(),
                request.status(),
                request.uf(),
                request.codRegiao(),
                request.rendaFamiliar(),
                request.codigoPrograma(),
                request.dependentes().stream()
                        .map(item -> new BeneficiaryService.DependentCommand(
                                item.cpf(), item.nome(), item.dataNascimento(), item.parentesco(), item.status(), item.indicadorDeficiencia()))
                        .toList()));
        return BeneficiaryResponse.from(saved);
    }

    @GetMapping("/{cpf}")
    public BeneficiaryResponse get(@PathVariable String cpf) {
        return BeneficiaryResponse.from(service.getByCpf(cpf));
    }

    @PostMapping("/{cpf}/biometria")
    public BeneficiaryResponse registerBiometrics(@PathVariable String cpf, @Valid @RequestBody RegisterBiometricsRequest request) {
        return BeneficiaryResponse.from(service.registerBiometrics(cpf,
                new BeneficiaryService.RegisterBiometricsCommand(request.hashTemplate(), request.codigoPosto())));
    }

    public record CreateBeneficiaryRequest(
            @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
            @NotBlank String nome,
            @NotNull LocalDate dataNascimento,
            @NotBlank @Pattern(regexp = "[ASCID]") String status,
            @NotBlank @Pattern(regexp = "[A-Z]{2}") String uf,
            @NotBlank @Pattern(regexp = "(0[1-5]|99)") String codRegiao,
            @NotNull @DecimalMin("0.00") BigDecimal rendaFamiliar,
            @NotBlank @Pattern(regexp = "[A-Z0-9]{4}") String codigoPrograma,
            @NotNull List<@Valid DependentRequest> dependentes) {
    }

    public record DependentRequest(
            @Pattern(regexp = "\\d{11}") String cpf,
            @NotBlank String nome,
            @NotNull LocalDate dataNascimento,
            @NotBlank @Pattern(regexp = "FI|CJ|NT|TU") String parentesco,
            @NotBlank @Pattern(regexp = "[AID]") String status,
            @NotBlank @Pattern(regexp = "[SN]") String indicadorDeficiencia) {
    }

    public record RegisterBiometricsRequest(
            @NotBlank @Pattern(regexp = "[a-fA-F0-9]{64}") String hashTemplate,
            @NotBlank String codigoPosto) {
    }

    public record BeneficiaryResponse(
            String cpf,
            String nome,
            String status,
            String uf,
            String codRegiao,
            BigDecimal rendaFamiliar,
            String codigoPrograma,
            String biometria,
            int totalDependentes) {

        static BeneficiaryResponse from(BeneficiaryEntity entity) {
            return new BeneficiaryResponse(
                    entity.getCpf(),
                    entity.getName(),
                    entity.getStatus(),
                    entity.getUf(),
                    entity.getRegionCode(),
                    entity.getFamilyIncome(),
                    entity.getProgram().getCode(),
                    entity.getBiometricsStatus(),
                    entity.getDependents().size());
        }
    }
}