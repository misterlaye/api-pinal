package com.dairy.apipinal.reproduction.infrastructure.persistence;

import com.dairy.apipinal.reproduction.domain.CycleReproduction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CycleReproductionRepository extends JpaRepository<CycleReproduction, UUID> {

    /**
     * Cycle de l'exploitation désignée : toute commande utilisateur charge un cycle par ce biais.
     */
    Optional<CycleReproduction> findByIdAndTenantIdAndExploitationId(UUID id, UUID tenantId, UUID exploitationId);

    List<CycleReproduction> findByAnimalIdAndTenantIdAndExploitationIdOrderByNumeroCycleDesc(
            UUID animalId,
            UUID tenantId,
            UUID exploitationId
    );

    List<CycleReproduction> findByExploitationId(UUID exploitationId);
}
