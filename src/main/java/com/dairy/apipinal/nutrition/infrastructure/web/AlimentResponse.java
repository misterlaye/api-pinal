package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.domain.Aliment;
import com.dairy.apipinal.nutrition.domain.UniteAliment;

import java.util.UUID;

public record AlimentResponse(
        UUID id,
        String code,
        String nom,
        String categorie,
        UniteAliment unite,
        boolean actif
) {
    public static AlimentResponse from(Aliment aliment) {
        return new AlimentResponse(
                aliment.getId(),
                aliment.getCode(),
                aliment.getNom(),
                aliment.getCategorie(),
                aliment.getUnite(),
                aliment.isActif()
        );
    }
}
