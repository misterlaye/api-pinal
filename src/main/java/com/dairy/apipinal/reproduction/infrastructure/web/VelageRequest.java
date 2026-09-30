package com.dairy.apipinal.reproduction.infrastructure.web;

import java.time.LocalDate;
import java.util.List;

public record VelageRequest(
        LocalDate dateReelle,
        List<VeauPayload> veaux
) {
    public record VeauPayload(
            String identifiant,
            String nom,
            String sexe,
            int indexPortee
    ) {}
}
