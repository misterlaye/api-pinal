package com.dairy.apipinal.reproduction.infrastructure.web;

import java.time.LocalDate;

public record ConstatGestationRequest(
        LocalDate dateConstat,
        String resultat,
        String veterinaire
) {
}
