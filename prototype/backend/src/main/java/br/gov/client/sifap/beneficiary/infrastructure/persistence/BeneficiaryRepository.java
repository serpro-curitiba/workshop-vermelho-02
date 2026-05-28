package br.gov.client.sifap.beneficiary.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BeneficiaryRepository extends JpaRepository<BeneficiaryEntity, Long> {

    Optional<BeneficiaryEntity> findByCpf(String cpf);

    boolean existsByCpf(String cpf);
}