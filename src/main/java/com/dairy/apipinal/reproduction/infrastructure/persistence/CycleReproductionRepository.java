package com.dairy.apipinal.reproduction.infrastructure.persistence;

import com.dairy.apipinal.reproduction.domain.CycleReproduction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CycleReproductionRepository extends JpaRepository<CycleReproduction, UUID> {
    List<CycleReproduction> findByAnimalIdOrderByNumeroCycleDesc(UUID animalId);
    List<CycleReproduction> findByExploitationId(UUID exploitationId);
}
