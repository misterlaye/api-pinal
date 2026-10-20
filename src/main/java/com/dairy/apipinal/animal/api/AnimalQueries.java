package com.dairy.apipinal.animal.api;

import java.util.UUID;

public interface AnimalQueries {

    /**
     * Animal de l'exploitation courante (contexte de la requête).
     * @throws IllegalArgumentException si l'animal n'existe pas dans cette exploitation.
     */
    AnimalReference getReference(UUID animalId);

    /**
     * Animal d'une exploitation explicitement désignée, pour les traitements indirects
     * (écouteurs d'événements) qui ne disposent pas du contexte de la requête.
     */
    AnimalReference getReference(UUID animalId, UUID tenantId, UUID exploitationId);

    boolean exists(UUID animalId);

    boolean isActive(UUID animalId);

    boolean isEligibleForProduction(UUID animalId);
}
