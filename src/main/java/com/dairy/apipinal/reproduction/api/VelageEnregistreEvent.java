package com.dairy.apipinal.reproduction.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VelageEnregistreEvent(
        UUID cycleId,
        UUID animalId,
        UUID tenantId,
        UUID exploitationId,
        LocalDate dateVelage,
        List<VeauPayload> veaux,
        UUID taureauId,
        String codePaillette,
        UUID actorId
) {
    public record VeauPayload(
            String identifiant,
            String nom,
            String sexe,
            int indexPortee
    ) {}
}
