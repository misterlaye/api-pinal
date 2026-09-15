package com.dairy.apipinal.health.api;

import java.util.List;
import java.util.UUID;

public interface HealthQueries {

    HealthEventReference getReference(UUID healthEventId);

    List<HealthEventReference> getAnimalHistory(UUID animalId);
}