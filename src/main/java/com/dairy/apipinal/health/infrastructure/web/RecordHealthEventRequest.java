package com.dairy.apipinal.health.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record RecordHealthEventRequest(

        @NotNull
        OffsetDateTime dateHeure,

        @NotBlank
        @Size(max = 2000)
        String description
) {
}