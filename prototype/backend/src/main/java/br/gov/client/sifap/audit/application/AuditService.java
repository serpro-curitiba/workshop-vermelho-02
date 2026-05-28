package br.gov.client.sifap.audit.application;

import br.gov.client.sifap.audit.infrastructure.persistence.AuditEventEntity;
import br.gov.client.sifap.audit.infrastructure.persistence.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void log(String actionCode, String entityType, String entityId, String description) {
        AuditEventEntity entity = new AuditEventEntity();
        entity.setActionCode(actionCode);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setDescription(description);
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<AuditEventEntity> list(String actionCode) {
        if (actionCode == null || actionCode.isBlank()) {
            return repository.findAllByOrderByCreatedAtDesc();
        }
        return repository.findByActionCodeOrderByCreatedAtDesc(actionCode);
    }
}