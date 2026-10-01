package com.dairy.apipinal.aicopilot.infrastructure.gateway;

import com.dairy.apipinal.aicopilot.api.Evidence;
import com.dairy.apipinal.aicopilot.api.SupportLevel;
import com.dairy.apipinal.aicopilot.api.TemporalRelation;
import com.dairy.apipinal.aicopilot.application.LLMOutput;
import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LLMGatewayAdapterTest {

    private final LLMGatewayAdapter adapter = new LLMGatewayAdapter();

    @Test
    void throws_when_context_is_empty() {
        AnomalyAlert anomaly = new AnomalyAlert("id1", "CODE", "HIGH", "Titre", "Desc", UUID.randomUUID(), OffsetDateTime.now());
        EvidenceContext context = EvidenceContext.build(anomaly, List.of());

        assertThrows(RuntimeException.class, () -> adapter.interpret(context));
    }

    @Test
    void returns_output_from_an_explicit_test_provider() {
        AnomalyAlert anomaly = new AnomalyAlert("id1", "CODE", "HIGH", "Titre", "Desc", UUID.randomUUID(), OffsetDateTime.now());
        Evidence ev1 = createEvidence("ev-1");
        EvidenceContext context = EvidenceContext.build(anomaly, List.of(ev1));

        adapter.setCustomProvider(ignored -> new LLMOutput(
                List.of(new LLMOutput.Hypothesis(
                        "Les faits observÃ©s coÃ¯ncident avec la baisse.",
                        List.of("ev-1")
                )),
                List.of("VET_CONSULT")
        ));
        LLMOutput output = adapter.interpret(context);

        assertNotNull(output);
        assertFalse(output.hypotheses().isEmpty());
        assertTrue(output.hypotheses().get(0).referencedEvidenceIds().contains("ev-1"));
        assertTrue(output.suggestedActionCodes().contains("VET_CONSULT"));
    }

    private Evidence createEvidence(String id) {
        return new Evidence() {
            @Override public String evidenceId() { return id; }
            @Override public String contentDigest() { return "digest"; }
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
