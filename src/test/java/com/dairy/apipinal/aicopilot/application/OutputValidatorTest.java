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

class OutputValidatorTest {

    private final ActionPolicyService actionPolicyService = new ActionPolicyService();
    private final OutputValidator validator = new OutputValidator(actionPolicyService);

    @Test
    void rejects_null_output() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(null, createContext()));
    }

    @Test
    void filters_unsupported_evidence_references() {
        EvidenceContext context = createContext("ev-1", "ev-2");
        
        LLMOutput rawOutput = new LLMOutput(
                List.of(
                        new LLMOutput.Hypothesis("Valid", List.of("ev-1")),
                        new LLMOutput.Hypothesis("Invalid grounding", List.of("ev-3"))
                ),
                List.of()
        );

        LLMOutput validated = validator.validate(rawOutput, context);
        
        assertEquals(1, validated.hypotheses().size());
        assertEquals("Valid", validated.hypotheses().get(0).description());
    }

    @Test
    void filters_invalid_action_codes() {
        EvidenceContext context = createContext("ev-1");

        LLMOutput rawOutput = new LLMOutput(
                List.of(new LLMOutput.Hypothesis("Valid", List.of("ev-1"))),
                List.of("VET_CONSULT", "DROP_TABLE_ANIMAL")
        );

        LLMOutput validated = validator.validate(rawOutput, context);

        assertEquals(1, validated.suggestedActionCodes().size());
        assertTrue(validated.suggestedActionCodes().contains("VET_CONSULT"));
        assertFalse(validated.suggestedActionCodes().contains("DROP_TABLE_ANIMAL"));
    }

    @Test
    void throws_if_no_valid_hypotheses_remain() {
        EvidenceContext context = createContext("ev-1");

        LLMOutput rawOutput = new LLMOutput(
                List.of(new LLMOutput.Hypothesis("Invalid grounding", List.of("ev-999"))),
                List.of("VET_CONSULT")
        );

        assertThrows(IllegalStateException.class, () -> validator.validate(rawOutput, context));
    }

    private EvidenceContext createContext(String... evidenceIds) {
        OffsetDateTime now = OffsetDateTime.now();
        AnomalyAlert anomaly = new AnomalyAlert("id1", "DROP_MILK_PRODUCTION", "HIGH", "Titre", "Desc", UUID.randomUUID(), now);
        List<Evidence> evidences = java.util.Arrays.stream(evidenceIds).map(id -> createEvidence(id, now.minusHours(1))).toList();
        return EvidenceContext.build(anomaly, evidences);
    }

    private Evidence createEvidence(String id, OffsetDateTime obsDate) {
        return new Evidence() {
            @Override public String evidenceId() { return id; }
            @Override public String contentDigest() { return "digest"; }
            @Override public String sourceModule() { return "MODULE"; }
            @Override public String entityType() { return "TYPE"; }
            @Override public String entityId() { return "123"; }
            @Override public OffsetDateTime observationDate() { return obsDate; }
            @Override public String factType() { return "FACT"; }
            @Override public Map<String, String> values() { return Map.of(); }
            @Override public String provenance() { return "PROV"; }
            @Override public TemporalRelation temporalRelation() { return TemporalRelation.ANTE_ANOMALY; }
            @Override public SupportLevel supportLevel() { return SupportLevel.STRONG; }
        };
    }
}
