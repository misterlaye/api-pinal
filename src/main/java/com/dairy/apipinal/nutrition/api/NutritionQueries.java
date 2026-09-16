package com.dairy.apipinal.nutrition.api;

import java.util.UUID;

public interface NutritionQueries {

    RationReference getRation(UUID rationId);

    boolean exists(UUID rationId);
}