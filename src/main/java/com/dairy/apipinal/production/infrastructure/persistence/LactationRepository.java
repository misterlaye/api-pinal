package com.dairy.apipinal.production.infrastructure.persistence;

import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.production.domain.StatutLactation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LactationRepository extends JpaRepository<Lactation, UUID> {

    Optional<Lactation> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );

    boolean existsByAnimalIdAndTenantIdAndStatut(
            UUID animalId,
            UUID tenantId,
            StatutLactation statut
    );

    Optional<Lactation> findByAnimalIdAndTenantIdAndStatut(
            UUID animalId,
            UUID tenantId,
            StatutLactation statut
    );
}