package com.dairy.apipinal.aicopilot.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CopilotJobView(
        UUID jobId,
        String anomalyId,
        String status,
        String abstentionReason,
        String abstentionMessage,
        CopilotResult result,
        OffsetDateTime generatedAt
) {}
