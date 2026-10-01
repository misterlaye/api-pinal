package com.dairy.apipinal.aicopilot.application;

import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import java.util.UUID;

public record JobCreatedEvent(
        UUID jobId,
        UUID tenantId,
        UUID exploitationId,
        UUID animalId,
        AnomalyAlert anomaly
) {}
