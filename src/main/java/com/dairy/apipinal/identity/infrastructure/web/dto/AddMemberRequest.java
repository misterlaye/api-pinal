package com.dairy.apipinal.identity.infrastructure.web.dto;

import com.dairy.apipinal.identity.domain.RoleExploitation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddMemberRequest(
        @NotBlank
        @Size(max = 20)
        String telephone,

        @NotNull
        RoleExploitation role
) {}
