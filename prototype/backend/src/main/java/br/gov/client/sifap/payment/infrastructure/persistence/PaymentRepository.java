package br.gov.client.sifap.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByBeneficiaryCpfAndCompetence(String beneficiaryCpf, String competence);
}