package com.dairy.apipinal.animal.infrastructure.persistence;

import com.dairy.apipinal.animal.domain.Animal;
import com.dairy.apipinal.animal.domain.StatutAnimal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnimalRepository extends JpaRepository<Animal, UUID> {

    boolean existsByExploitationIdAndIdentifiant(
            UUID exploitationId,
            String identifiant
    );

    boolean existsByExploitationIdAndIdentifiantAndIdNot(
            UUID exploitationId,
            String identifiant,
            UUID animalId
    );

    Optional<Animal> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );

    List<Animal> findAllByTenantIdAndExploitationIdOrderByIdentifiantAsc(
            UUID tenantId,
            UUID exploitationId
    );

    boolean existsByIdAndTenantIdAndStatut(
            UUID id,
            UUID tenantId,
            StatutAnimal statut
    );
}
