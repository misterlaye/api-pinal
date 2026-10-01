package com.dairy.apipinal.aicopilot.infrastructure.providers;

import com.dairy.apipinal.aicopilot.api.Evidence;
import com.dairy.apipinal.aicopilot.api.EvidenceProvider;
import com.dairy.apipinal.aicopilot.api.SupportLevel;
import com.dairy.apipinal.aicopilot.api.TemporalRelation;
import com.dairy.apipinal.aicopilot.domain.HashUtils;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import com.dairy.apipinal.health.api.HealthEventReference;
import com.dairy.apipinal.health.api.HealthQueries;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class HealthEvidenceProvider implements EvidenceProvider {

    private final HealthQueries healthQueries;

    public HealthEvidenceProvider(HealthQueries healthQueries) {
        this.healthQueries = healthQueries;
    }

    @Override
    public List<Evidence> gatherEvidences(UUID tenantId, UUID exploitationId, UUID animalId, AnomalyAlert anomaly) {
        if (animalId == null) {
            return List.of();
        }
        List<HealthEventReference> history = healthQueries.getAnimalHistory(tenantId, exploitationId, animalId);
        List<Evidence> evidences = new ArrayList<>();

        for (HealthEventReference event : history) {
            long daysDifference = ChronoUnit.DAYS.between(event.dateHeure(), anomaly.dateDetection());

            // Only consider events within 30 days before, and up to anomaly date
            if (daysDifference >= 0 && daysDifference <= 30) {
                evidences.add(mapToEvidence(event, anomaly.dateDetection(), daysDifference));
            }
        }
        return evidences;
    }

    private Evidence mapToEvidence(HealthEventReference event, OffsetDateTime anomalyDate, long daysBeforeAnomaly) {
        String sourceModule = "HEALTH";
        String entityType = "EVENEMENT_SANITAIRE";
        String factType = "HEALTH_EVENT";
        String factVersion = "1";
        
        String evidenceIdInput = String.format("%s|%s|%s|%s|%s|%s",
                sourceModule, entityType, event.id().toString(), factType, factVersion, HashUtils.formatDateTimeCanonical(event.dateHeure()));
        String evidenceId = HashUtils.sha256(evidenceIdInput);

        Map<String, String> values = Map.of(
                "description", event.description() != null ? event.description() : "",
                "diagnostic", event.diagnostic() != null ? event.diagnostic() : "",
                "traitement", event.traitement() != null ? event.traitement() : ""
        );

        String canonicalValues = String.format("%s|%s|%s", values.get("description"), values.get("diagnostic"), values.get("traitement"));
        String contentDigest = HashUtils.sha256(canonicalValues);

        TemporalRelation temporalRelation = daysBeforeAnomaly == 0 ? TemporalRelation.CONCURRENT : TemporalRelation.ANTE_ANOMALY;
        
        SupportLevel supportLevel;
        if (daysBeforeAnomaly <= 7) {
            supportLevel = SupportLevel.STRONG;
        } else if (daysBeforeAnomaly <= 14) {
            supportLevel = SupportLevel.MODERATE;
        } else {
            supportLevel = SupportLevel.WEAK;
        }

        return new EvidenceRecord(
                evidenceId, contentDigest, sourceModule, entityType, event.id().toString(),
                event.dateHeure(), factType, values, "SYSTEM", temporalRelation, supportLevel
        );
    }
}

record EvidenceRecord(
        String evidenceId,
        String contentDigest,
        String sourceModule,
        String entityType,
        String entityId,
        OffsetDateTime observationDate,
        String factType,
        Map<String, String> values,
        String provenance,
        TemporalRelation temporalRelation,
        SupportLevel supportLevel
) implements Evidence {}
