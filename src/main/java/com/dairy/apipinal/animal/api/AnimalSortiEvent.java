package com.dairy.apipinal.animal.api;

import com.dairy.apipinal.animal.domain.StatutAnimal;
import java.time.LocalDate;
import java.util.UUID;

public record AnimalSortiEvent(
        UUID animalId,
        UUID tenantId,
        UUID exploitationId,
        StatutAnimal nouveauStatut,
        LocalDate dateSortie,
        UUID actorId
) {}
