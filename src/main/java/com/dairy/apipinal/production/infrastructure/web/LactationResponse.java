package com.dairy.apipinal.production.infrastructure.web;

import com.dairy.apipinal.production.domain.Lactation;

import java.time.LocalDate;
import java.util.UUID;

public record LactationResponse(
        UUID id,
        UUID animalId,
        LocalDate dateDebut,
        LocalDate dateFin,
        String statut
) {

    public static LactationResponse from(Lactation lactation) {
        return new LactationResponse(
                lactation.getId(),
                lactation.getAnimalId(),
                lactation.getDateDebut(),
                lactation.getDateFin(),
                lactation.getStatut().name()
        );
    }
}