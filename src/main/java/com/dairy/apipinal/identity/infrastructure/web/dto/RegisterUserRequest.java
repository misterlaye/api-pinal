package com.dairy.apipinal.identity.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @Size(max = 20)
        String telephone,

        @NotBlank
        @Size(max = 120)
        String nom,

        @NotBlank
        @Size(max = 120)
        String prenom,

        @Size(max = 320)
        String email
) {}
