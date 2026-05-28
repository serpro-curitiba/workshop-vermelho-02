package br.gov.client.sifap.audit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {

    List<AuditEventEntity> findByActionCodeOrderByCreatedAtDesc(String actionCode);

    List<AuditEventEntity> findAllByOrderByCreatedAtDesc();
}