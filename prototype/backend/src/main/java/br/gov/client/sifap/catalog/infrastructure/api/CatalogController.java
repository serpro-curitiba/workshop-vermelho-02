package br.gov.client.sifap.catalog.infrastructure.api;

import br.gov.client.sifap.catalog.application.CatalogService;
import br.gov.client.sifap.catalog.infrastructure.persistence.SocialProgramEntity;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1/programas")
public class CatalogController {

    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProgramResponse create(@Valid @RequestBody CreateProgramRequest request) {
        SocialProgramEntity saved = service.createProgram(new CatalogService.CreateProgramCommand(
                request.codigo(),
                request.nome(),
                request.tipo(),
                request.valorBase(),
                request.fatorReajuste(),
                Boolean.TRUE.equals(request.exigeBiometria()),
                request.fatoresRegionais().stream()
                        .map(item -> new CatalogService.RegionalFactorCommand(item.uf(), item.fatorRegional()))
                        .toList()));
        return ProgramResponse.from(saved);
    }

    @GetMapping("/{codigo}")
    public ProgramResponse get(@PathVariable String codigo) {
        return ProgramResponse.from(service.getProgram(codigo));
    }

    @GetMapping
    public List<ProgramResponse> list() {
        return service.listPrograms().stream().map(ProgramResponse::from).toList();
    }

    public record CreateProgramRequest(
            @NotBlank @Pattern(regexp = "[A-Z0-9]{4}") String codigo,
            @NotBlank String nome,
            @NotBlank @Pattern(regexp = "[ATP]") String tipo,
            @NotNull @DecimalMin("0.01") BigDecimal valorBase,
            @NotNull @DecimalMin("0.00") BigDecimal fatorReajuste,
            Boolean exigeBiometria,
            @NotNull List<@Valid RegionalFactorRequest> fatoresRegionais) {
    }

    public record RegionalFactorRequest(
            @NotBlank @Pattern(regexp = "[A-Z]{2}") String uf,
            @NotNull @DecimalMin("0.0001") BigDecimal fatorRegional) {
    }

    public record ProgramResponse(
            String codigo,
            String nome,
            String tipo,
            BigDecimal valorBaseOriginal,
            BigDecimal valorBaseAjustado,
            BigDecimal fatorReajuste,
            BigDecimal fatorK,
            boolean exigeBiometria,
            List<RegionalFactorResponse> fatoresRegionais) {

        static ProgramResponse from(SocialProgramEntity entity) {
            return new ProgramResponse(
                    entity.getCode(),
                    entity.getName(),
                    entity.getProgramType(),
                    entity.getBaseValueOriginal(),
                    entity.getBaseValueAdjusted(),
                    entity.getFactorReajuste(),
                    entity.getFactorK(),
                    entity.isBiometricsRequired(),
                    entity.getRegionalParameters().stream()
                            .map(item -> new RegionalFactorResponse(item.getUf(), item.getFactorRegional()))
                            .toList());
        }
    }

    public record RegionalFactorResponse(String uf, BigDecimal fatorRegional) {
    }
}