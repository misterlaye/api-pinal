package com.dairy.apipinal.animal.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Données nécessaires à l'inscription des veaux dans le troupeau. */
public record NaissancesDeclareesEvent(
        UUID cycleId,
        UUID animalId,
        UUID tenantId,
        UUID exploitationId,
        LocalDate dateVelage,
        List<VeauPayload> veaux,
        UUID taureauId,
        String codePaillette
) {
    public record VeauPayload(
            String identifiant,
            String nom,
            String sexe,
            int indexPortee
    ) {}
}
