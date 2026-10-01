package com.dairy.apipinal.aicopilot.application;

import com.dairy.apipinal.aicopilot.api.Evidence;
import com.dairy.apipinal.aicopilot.api.EvidenceProvider;
import com.dairy.apipinal.aicopilot.api.SupportLevel;
import com.dairy.apipinal.aicopilot.api.TemporalRelation;
import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceOrchestratorTest {

    @Test
    void gathers_and_filters_post_anomaly_evidences() {
        OffsetDateTime anomalyDate = OffsetDateTime.now();
        EvidenceProvider provider = (tId, eId, aId, anomaly) -> List.of(
                createEvidence("ev-1", anomalyDate.minusHours(1), TemporalRelation.ANTE_ANOMALY),
                createEvidence("ev-2", anomalyDate, TemporalRelation.CONCURRENT),
                createEvidence("ev-3", anomalyDate.plusHours(1), TemporalRelation.POST_ANOMALY)
        );

        EvidenceOrchestrator orchestrator = new EvidenceOrchestrator(List.of(provider));
        
        AnomalyAlert anomaly = new AnomalyAlert("id1", "CODE", "HIGH", "Titre", "Desc", UUID.randomUUID(), anomalyDate);
        EvidenceContext context = orchestrator.gatherContext(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), anomaly);

        assertEquals(2, context.evidences().size());
        assertTrue(context.evidences().stream().noneMatch(e -> e.temporalRelation() == TemporalRelation.POST_ANOMALY));
    }

    private Evidence createEvidence(String id, OffsetDateTime observationDate, TemporalRelation temporalRelation) {
        return new Evidence() {
            @Override public String evidenceId() { return id; }
            @Override public String contentDigest() { return "digest"; }
            @Override public String sourceModule() { return "MODULE"; }
            @Override public String entityType() { return "TYPE"; }
            @Override public String entityId() { return "123"; }
            @Override public OffsetDateTime observationDate() { return observationDate; }
            @Override public String factType() { return "FACT"; }
            @Override public Map<String, String> values() { return Map.of(); }
            @Override public String provenance() { return "PROV"; }
            @Override public TemporalRelation temporalRelation() { return temporalRelation; }
            @Override public SupportLevel supportLevel() { return SupportLevel.STRONG; }
        };
    }
}
