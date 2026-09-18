package com.dairy.apipinal.anomaly.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AnomalyAlert(
        String id,
        String code,
        String severite,
        String titre,
        String description,
        UUID animalId,
        OffsetDateTime dateDetection
) {}
