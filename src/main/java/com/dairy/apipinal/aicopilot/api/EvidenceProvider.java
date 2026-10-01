package com.dairy.apipinal.aicopilot.api;

import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import java.util.List;
import java.util.UUID;

public interface EvidenceProvider {
    List<Evidence> gatherEvidences(UUID tenantId, UUID exploitationId, UUID animalId, AnomalyAlert anomaly);
}
