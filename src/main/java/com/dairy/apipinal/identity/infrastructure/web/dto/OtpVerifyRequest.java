package com.dairy.apipinal.identity.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpVerifyRequest(
        @NotBlank
        @Size(max = 20)
        String telephone,

        @NotBlank
        @Size(max = 10)
        String code
) {}
