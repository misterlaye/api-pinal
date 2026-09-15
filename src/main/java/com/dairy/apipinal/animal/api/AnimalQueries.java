package com.dairy.apipinal.animal.api;

import java.util.UUID;

public interface AnimalQueries {

    AnimalReference getReference(UUID animalId);

    boolean exists(UUID animalId);

    boolean isActive(UUID animalId);

    boolean isEligibleForProduction(UUID animalId);
}
