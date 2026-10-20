package com.dairy.apipinal.reproduction.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record InseminationRequest(
        @NotNull
        UUID animalId,
        /** Facultatif : s'il est fourni, il doit correspondre à l'exploitation active. */
        UUID exploitationId,
        @NotNull
        @PastOrPresent
        LocalDate dateInsemination,
        @NotBlank
        String methodeReproduction,
        UUID taureauId,
        @Size(max = 100)
        String codePaillette
) {
}
