package com.dairy.apipinal.aicopilot.infrastructure.providers;

import com.dairy.apipinal.aicopilot.api.Evidence;
import com.dairy.apipinal.aicopilot.api.EvidenceProvider;
import com.dairy.apipinal.aicopilot.api.SupportLevel;
import com.dairy.apipinal.aicopilot.api.TemporalRelation;
import com.dairy.apipinal.aicopilot.domain.HashUtils;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import com.dairy.apipinal.nutrition.api.NutritionQueries;
import com.dairy.apipinal.nutrition.api.RationReference;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class NutritionEvidenceProvider implements EvidenceProvider {

    private final NutritionQueries nutritionQueries;

    public NutritionEvidenceProvider(NutritionQueries nutritionQueries) {
        this.nutritionQueries = nutritionQueries;
    }

    @Override
    public List<Evidence> gatherEvidences(UUID tenantId, UUID exploitationId, UUID animalId, AnomalyAlert anomaly) {
        if (animalId == null) {
            return List.of();
        }
        List<Evidence> evidences = new ArrayList<>();
        LocalDate anomalyDate = anomaly.dateDetection().toLocalDate();

        // Check active ration on anomaly date
        Optional<RationReference> activeRationOpt = nutritionQueries.findActiveRation(
                tenantId, exploitationId, animalId, anomalyDate);
        
        if (activeRationOpt.isPresent()) {
            RationReference activeRation = activeRationOpt.get();
            long daysSinceRationStart = ChronoUnit.DAYS.between(activeRation.dateDebut(), anomalyDate);
            
            // Only relevant if ration changed recently (within 14 days)
            if (daysSinceRationStart >= 0 && daysSinceRationStart <= 14) {
                evidences.add(mapToEvidence(activeRation, anomaly.dateDetection(), daysSinceRationStart));
            }
        }

        return evidences;
    }

    private Evidence mapToEvidence(RationReference ration, OffsetDateTime anomalyDate, long daysBeforeAnomaly) {
        String sourceModule = "NUTRITION";
        String entityType = "RATION";
        String factType = "RATION_ACTIVE";
        String factVersion = "1";
        
        OffsetDateTime observationDate = ration.dateDebut().atStartOfDay().atOffset(ZoneOffset.UTC);

        String evidenceIdInput = String.format("%s|%s|%s|%s|%s|%s",
                sourceModule, entityType, ration.id().toString(), factType, factVersion, HashUtils.formatDateTimeCanonical(observationDate));
        String evidenceId = HashUtils.sha256(evidenceIdInput);

        Map<String, String> values = Map.of(
                "statut", String.valueOf(ration.statut()),
                "origine", String.valueOf(ration.origine())
        );

        String canonicalValues = String.format("%s|%s", values.get("statut"), values.get("origine"));
        String contentDigest = HashUtils.sha256(canonicalValues);

        TemporalRelation temporalRelation = daysBeforeAnomaly == 0 ? TemporalRelation.CONCURRENT : TemporalRelation.ANTE_ANOMALY;
        
        SupportLevel supportLevel;
        if (daysBeforeAnomaly <= 3) {
            supportLevel = SupportLevel.STRONG; // Very recent ration change
        } else if (daysBeforeAnomaly <= 7) {
            supportLevel = SupportLevel.MODERATE;
        } else {
            supportLevel = SupportLevel.WEAK;
        }

        return new EvidenceRecord(
                evidenceId, contentDigest, sourceModule, entityType, ration.id().toString(),
                observationDate, factType, values, "SYSTEM", temporalRelation, supportLevel
        );
    }
}
