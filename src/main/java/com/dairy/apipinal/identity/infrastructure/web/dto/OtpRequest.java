package com.dairy.apipinal.identity.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpRequest(
        @NotBlank
        @Size(max = 20)
        String telephone
) {}
