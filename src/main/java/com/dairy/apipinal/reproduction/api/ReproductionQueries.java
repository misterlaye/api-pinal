package com.dairy.apipinal.reproduction.api;

import java.util.List;
import java.util.UUID;

public interface ReproductionQueries {
    List<CycleReproductionResponse> getCyclesByAnimal(UUID animalId);
}
