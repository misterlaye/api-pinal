package com.dairy.apipinal.reproduction.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ConstatGestationRequest(
        @NotNull
        @PastOrPresent
        LocalDate dateConstat,
        @NotBlank
        String resultat,
        @Size(max = 150)
        String veterinaire
) {
}
