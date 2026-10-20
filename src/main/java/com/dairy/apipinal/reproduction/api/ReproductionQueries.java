package com.dairy.apipinal.reproduction.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReproductionQueries {

    /** Cycles d'un animal de l'exploitation désignée, du plus récent au plus ancien. */
    List<CycleReproductionResponse> getCyclesByAnimal(UUID animalId, UUID tenantId, UUID exploitationId);

    Optional<CycleReproductionResponse> getCycle(UUID cycleId, UUID tenantId, UUID exploitationId);
}
