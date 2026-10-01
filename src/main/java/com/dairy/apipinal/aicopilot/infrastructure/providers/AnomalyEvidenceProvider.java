package com.dairy.apipinal.aicopilot.infrastructure.providers;

import com.dairy.apipinal.aicopilot.api.Evidence;
import com.dairy.apipinal.aicopilot.api.EvidenceProvider;
import com.dairy.apipinal.aicopilot.api.SupportLevel;
import com.dairy.apipinal.aicopilot.api.TemporalRelation;
import com.dairy.apipinal.aicopilot.domain.HashUtils;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Makes the already computed tenant-scoped anomaly available as an Evidence.
 * Exploitation-wide anomalies intentionally have no animal identifier.
 */
@Component
public class AnomalyEvidenceProvider implements EvidenceProvider {

    @Override
    public List<Evidence> gatherEvidences(UUID tenantId, UUID exploitationId, UUID animalId, AnomalyAlert anomaly) {
        String observation = HashUtils.formatDateTimeCanonical(anomaly.dateDetection());
        String evidenceId = HashUtils.sha256(
                "ANOMALY|ANOMALY_ALERT|" + anomaly.id() + "|PRODUCTION_ANOMALY|v1|" + observation
        );
        Map<String, String> values = Map.of(
                "code", anomaly.code(),
                "severity", anomaly.severite(),
                "title", anomaly.titre(),
                "observation", anomaly.description()
        );
        String contentDigest = HashUtils.sha256(String.join("|",
                anomaly.code(), anomaly.severite(), anomaly.titre(), anomaly.description()
        ));
        return List.of(new EvidenceRecord(
                evidenceId, contentDigest, "PRODUCTION", "ANOMALY_ALERT", anomaly.id(),
                anomaly.dateDetection(), "PRODUCTION_ANOMALY", values, "SYSTEM",
                TemporalRelation.CONCURRENT, SupportLevel.STRONG
        ));
    }
}
