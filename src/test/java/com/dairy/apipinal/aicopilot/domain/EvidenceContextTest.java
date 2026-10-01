package com.dairy.apipinal.aicopilot.domain;

import com.dairy.apipinal.aicopilot.api.Evidence;
import com.dairy.apipinal.aicopilot.api.SupportLevel;
import com.dairy.apipinal.aicopilot.api.TemporalRelation;
import com.dairy.apipinal.aicopilot.infrastructure.providers.AnomalyEvidenceProvider;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceContextTest {

    @Test
    void contextHash_is_independent_of_order() {
        AnomalyAlert anomaly = new AnomalyAlert("id1", "CODE", "HIGH", "Titre", "Desc", UUID.randomUUID(), OffsetDateTime.now());
        
        Evidence ev1 = createEvidence("ev-1", "digest-1");
        Evidence ev2 = createEvidence("ev-2", "digest-2");

        EvidenceContext ctx1 = EvidenceContext.build(anomaly, List.of(ev1, ev2));
        EvidenceContext ctx2 = EvidenceContext.build(anomaly, List.of(ev2, ev1));

        assertEquals(ctx1.contextHash(), ctx2.contextHash());
    }

    @Test
    void contextHash_changes_if_contentDigest_changes() {
        AnomalyAlert anomaly = new AnomalyAlert("id1", "CODE", "HIGH", "Titre", "Desc", UUID.randomUUID(), OffsetDateTime.now());
        
        Evidence ev1 = createEvidence("ev-1", "digest-1");
        Evidence ev2 = createEvidence("ev-1", "digest-modified");

        EvidenceContext ctx1 = EvidenceContext.build(anomaly, List.of(ev1));
        EvidenceContext ctx2 = EvidenceContext.build(anomaly, List.of(ev2));

        assertNotEquals(ctx1.contextHash(), ctx2.contextHash());
    }

    @Test
    void contextHash_ignores_a_recomputed_detection_timestamp_when_business_content_is_unchanged() {
        UUID animalId = UUID.randomUUID();
        AnomalyAlert first = new AnomalyAlert(
                "id1", "CODE", "HIGH", "Titre", "Desc", animalId, OffsetDateTime.parse("2026-10-01T10:00:00Z"));
        AnomalyAlert recomputed = new AnomalyAlert(
                "id1", "CODE", "HIGH", "Titre", "Desc", animalId, OffsetDateTime.parse("2026-10-01T10:00:05Z"));
        Evidence evidence = createEvidence("ev-1", "digest-1");

        assertEquals(
                EvidenceContext.build(first, List.of(evidence)).contextHash(),
                EvidenceContext.build(recomputed, List.of(evidence)).contextHash()
        );
    }

    @Test
    void anomalyEvidence_identity_is_stable_when_only_detection_timestamp_is_recomputed() {
        UUID tenantId = UUID.randomUUID();
        UUID exploitationId = UUID.randomUUID();
        AnomalyAlert first = new AnomalyAlert(
                "id1", "CODE", "HIGH", "Titre", "Desc", null, OffsetDateTime.parse("2026-10-01T10:00:00Z"));
        AnomalyAlert recomputed = new AnomalyAlert(
                "id1", "CODE", "HIGH", "Titre", "Desc", null, OffsetDateTime.parse("2026-10-01T10:00:05Z"));
        AnomalyEvidenceProvider provider = new AnomalyEvidenceProvider();

        Evidence firstEvidence = provider.gatherEvidences(tenantId, exploitationId, null, first).getFirst();
        Evidence recomputedEvidence = provider.gatherEvidences(tenantId, exploitationId, null, recomputed).getFirst();

        assertEquals(firstEvidence.evidenceId(), recomputedEvidence.evidenceId());
        assertEquals(firstEvidence.contentDigest(), recomputedEvidence.contentDigest());
    }

    private Evidence createEvidence(String id, String digest) {
        return new Evidence() {
            @Override public String evidenceId() { return id; }
            @Override public String contentDigest() { return digest; }
            @Override public String sourceModule() { return "MODULE"; }
            @Override public String entityType() { return "TYPE"; }
            @Override public String entityId() { return "123"; }
            @Override public OffsetDateTime observationDate() { return OffsetDateTime.now(); }
            @Override public String factType() { return "FACT"; }
            @Override public Map<String, String> values() { return Map.of(); }
            @Override public String provenance() { return "PROV"; }
            @Override public TemporalRelation temporalRelation() { return TemporalRelation.ANTE_ANOMALY; }
            @Override public SupportLevel supportLevel() { return SupportLevel.STRONG; }
        };
    }
}
