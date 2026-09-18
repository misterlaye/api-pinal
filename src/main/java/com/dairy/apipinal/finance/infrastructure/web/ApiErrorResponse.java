package com.dairy.apipinal.finance.infrastructure.web;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {}