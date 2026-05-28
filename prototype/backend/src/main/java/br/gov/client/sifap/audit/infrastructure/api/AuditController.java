package br.gov.client.sifap.audit.infrastructure.api;

import br.gov.client.sifap.audit.application.AuditService;
import br.gov.client.sifap.audit.infrastructure.persistence.AuditEventEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auditoria")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditResponse> list(@RequestParam(required = false) String acao) {
        return service.list(acao).stream().map(AuditResponse::from).toList();
    }

    public record AuditResponse(Long id, String acao, String entidade, String entidadeId, String descricao, Instant criadoEm) {
        static AuditResponse from(AuditEventEntity entity) {
            return new AuditResponse(entity.getId(), entity.getActionCode(), entity.getEntityType(), entity.getEntityId(), entity.getDescription(), entity.getCreatedAt());
        }
    }
}