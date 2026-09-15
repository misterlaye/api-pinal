package com.dairy.apipinal.health.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateHealthEventRequest(

        @NotBlank
        @Size(max = 2000)
        String description,

        @Size(max = 2000)
        String diagnostic,

        @Size(max = 2000)
        String traitement,

        LocalDate dateFin
) {
}