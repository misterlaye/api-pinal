package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.domain.UniteAliment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAlimentRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String nom,
        @NotBlank @Size(max = 100) String categorie,
        @NotNull UniteAliment unite
) {
}
