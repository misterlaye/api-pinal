package com.dairy.apipinal.reproduction.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record VelageRequest(
        @NotNull
        @PastOrPresent
        LocalDate dateReelle,
        List<@Valid VeauPayload> veaux
) {
    public record VeauPayload(
            @NotBlank
            @Size(max = 100)
            String identifiant,
            @Size(max = 150)
            String nom,
            String sexe,
            int indexPortee
    ) {}
}
