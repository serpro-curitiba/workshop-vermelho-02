package br.gov.client.sifap.catalog.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SocialProgramRepository extends JpaRepository<SocialProgramEntity, String> {

    List<SocialProgramEntity> findAllByOrderByCodeAsc();
}