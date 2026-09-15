package com.dairy.apipinal.health.infrastructure.persistence;

import com.dairy.apipinal.health.domain.EvenementSanitaire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvenementSanitaireRepository
        extends JpaRepository<EvenementSanitaire, UUID> {

    Optional<EvenementSanitaire> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );

    List<EvenementSanitaire>
    findAllByAnimalIdAndTenantIdOrderByDateHeureDesc(
            UUID animalId,
            UUID tenantId
    );
}