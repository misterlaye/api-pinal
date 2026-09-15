package com.dairy.apipinal.animal.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateAnimalRequest(

        @NotNull
        UUID raceId,

        @NotBlank
        @Size(max = 100)
        String identifiant,

        @NotBlank
        @Size(max = 150)
        String nom,

        @Size(max = 500)
        String photoUrl,

        @Past
        LocalDate dateNaissance
) {}
