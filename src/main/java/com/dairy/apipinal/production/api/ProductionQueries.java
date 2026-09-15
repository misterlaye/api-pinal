package com.dairy.apipinal.production.api;

import java.util.UUID;

public interface ProductionQueries {
    ProductionReference getActiveLactation(UUID animalId);
}