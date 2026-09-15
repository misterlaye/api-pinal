package com.dairy.apipinal.animal.infrastructure.web;

import com.dairy.apipinal.animal.domain.Animal;

import java.time.LocalDate;
import java.util.UUID;

public record AnimalResponse(
        UUID id,
        UUID tenantId,
        UUID exploitationId,
        UUID raceId,
        String identifiant,
        String nom,
        String photoUrl,
        LocalDate dateNaissance,
        String statut
) {

    public static AnimalResponse from(Animal animal) {
        return new AnimalResponse(
                animal.getId(),
                animal.getTenantId(),
                animal.getExploitationId(),
                animal.getRaceId(),
                animal.getIdentifiant(),
                animal.getNom(),
                animal.getPhotoUrl(),
                animal.getDateNaissance(),
                animal.getStatut().name()
        );
    }
}
