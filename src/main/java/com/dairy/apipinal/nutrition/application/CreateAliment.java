package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.nutrition.domain.UniteAliment;

public record CreateAliment(
        String code,
        String nom,
        String categorie,
        UniteAliment unite
) {
}
