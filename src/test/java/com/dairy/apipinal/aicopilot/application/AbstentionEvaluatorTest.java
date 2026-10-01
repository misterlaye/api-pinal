package com.dairy.apipinal.aicopilot.application;

import com.dairy.apipinal.aicopilot.api.Evidence;
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

class AbstentionEvaluatorTest {

    private final AbstentionEvaluator evaluator = new AbstentionEvaluator();

    @Test
    void evaluates_NO_EVIDENCE_FOUND_when_empty() {
        EvidenceContext ctx = EvidenceContext.build(createAnomaly(), List.of());
        assertEquals(AbstentionEvaluator.AbstentionReason.NO_EVIDENCE_FOUND, evaluator.evaluate(ctx));
    }

    @Test
    void evaluates_LOW_SIGNAL_when_only_weak() {
        EvidenceContext ctx = EvidenceContext.build(createAnomaly(), List.of(createEvidence(SupportLevel.WEAK)));
        assertEquals(AbstentionEvaluator.AbstentionReason.LOW_SIGNAL, evaluator.evaluate(ctx));
    }

    @Test
    void evaluates_NONE_when_moderate_or_strong_exists() {
        EvidenceContext ctx1 = EvidenceContext.build(createAnomaly(), List.of(createEvidence(SupportLevel.MODERATE)));
        assertEquals(AbstentionEvaluator.AbstentionReason.NONE, evaluator.evaluate(ctx1));

        EvidenceContext ctx2 = EvidenceContext.build(createAnomaly(), List.of(createEvidence(SupportLevel.WEAK), createEvidence(SupportLevel.STRONG)));
        assertEquals(AbstentionEvaluator.AbstentionReason.NONE, evaluator.evaluate(ctx2));
    }

    private AnomalyAlert createAnomaly() {
        return new AnomalyAlert("id1", "CODE", "HIGH", "Titre", "Desc", UUID.randomUUID(), OffsetDateTime.now());
    }

    private Evidence createEvidence(SupportLevel level) {
        return new Evidence() {
            @Override public String evidenceId() { return UUID.randomUUID().toString(); }
            @Override public String contentDigest() { return "digest"; }
            @Override public String sourceModule() { return "MODULE"; }
            @Override public String entityType() { return "TYPE"; }
            @Override public String entityId() { return "123"; }
            @Override public OffsetDateTime observationDate() { return OffsetDateTime.now(); }
            @Override public String factType() { return "FACT"; }
            @Override public Map<String, String> values() { return Map.of(); }
            @Override public String provenance() { return "PROV"; }
            @Override public TemporalRelation temporalRelation() { return TemporalRelation.ANTE_ANOMALY; }
            @Override public SupportLevel supportLevel() { return level; }
        };
    }
}
