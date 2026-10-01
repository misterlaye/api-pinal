package com.dairy.apipinal.aicopilot;

import com.dairy.apipinal.TestcontainersConfiguration;
import com.dairy.apipinal.aicopilot.api.Evidence;
import com.dairy.apipinal.aicopilot.api.SupportLevel;
import com.dairy.apipinal.aicopilot.api.TemporalRelation;
import com.dairy.apipinal.aicopilot.application.EvidenceOrchestrator;
import com.dairy.apipinal.aicopilot.application.LLMOutput;
import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import com.dairy.apipinal.aicopilot.infrastructure.gateway.AIInterpreter;
import com.dairy.apipinal.aicopilot.infrastructure.persistence.AnomalyInterpretationJob;
import com.dairy.apipinal.aicopilot.infrastructure.persistence.AnomalyInterpretationJobRepository;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import com.dairy.apipinal.anomaly.api.AnomalyQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "pinal.ai-copilot.enabled=true")
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AICopilotProductFlowIT {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AnomalyInterpretationJobRepository repository;

    @MockitoBean private AnomalyQueries anomalyQueries;
    @MockitoBean private EvidenceOrchestrator evidenceOrchestrator;
    @MockitoBean private AIInterpreter aiInterpreter;
    @MockitoBean private TenantContext tenantContext;

    private MockMvc mockMvc;
    private UUID tenantId;
    private UUID exploitationId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        reset(anomalyQueries, evidenceOrchestrator, aiInterpreter, tenantContext);
        tenantId = UUID.randomUUID();
        exploitationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        when(tenantContext.currentTenantId()).thenReturn(tenantId);
        when(tenantContext.currentExploitationId()).thenReturn(exploitationId);
        when(tenantContext.currentUserId()).thenReturn(userId);
        when(tenantContext.currentRole()).thenReturn("PROPRIETAIRE");
        when(aiInterpreter.getPromptVersion()).thenReturn("v1");
        when(aiInterpreter.getModelVersion()).thenReturn("test-model");
        when(aiInterpreter.getOutputSchemaVersion()).thenReturn("v1");
    }

    @Test
    void nominal_double_click_completed_and_stale_refresh_contract() throws Exception {
        AnomalyAlert anomaly = anomaly("nominal");
        Evidence evidence = evidence("health-1", "digest-1");
        EvidenceContext firstContext = EvidenceContext.build(anomaly, List.of(evidence));
        when(anomalyQueries.detectAnomalies()).thenReturn(List.of(anomaly));
        when(evidenceOrchestrator.gatherContext(any(), any(), any(), any())).thenReturn(firstContext);
        LLMOutput validatedOutput = new LLMOutput(
                List.of(new LLMOutput.Hypothesis("Un facteur sanitaire pourrait être associé.", List.of("health-1"))),
                List.of("VET_CONSULT"));
        when(aiInterpreter.interpret(firstContext)).thenReturn(validatedOutput);

        UUID firstJob = request(anomaly.id());
        UUID secondJob = request(anomaly.id());
        assertEquals(firstJob, secondJob);

        JsonNode completed = awaitTerminal(firstJob);
        assertEquals("COMPLETED", completed.get("status").asText());
        assertEquals("Consulter un vétérinaire", completed.at("/result/suggestedActions/0/label").asText());
        assertFalse(completed.toString().contains("health-1"));
        verify(aiInterpreter, times(1)).interpret(any());

        EvidenceContext changedContext = EvidenceContext.build(anomaly, List.of(evidence("health-1", "digest-2")));
        when(evidenceOrchestrator.gatherContext(any(), any(), any(), any())).thenReturn(changedContext);
        when(aiInterpreter.interpret(changedContext)).thenReturn(validatedOutput);

        JsonNode stale = getJob(firstJob);
        assertEquals("STALE", stale.get("status").asText());
        assertFalse(stale.hasNonNull("result"));

        UUID refreshedJob = request(anomaly.id());
        org.junit.jupiter.api.Assertions.assertNotEquals(firstJob, refreshedJob);
        assertEquals("COMPLETED", awaitTerminal(refreshedJob).get("status").asText());
    }

    @Test
    void insufficient_data_abstains_without_calling_the_model() throws Exception {
        AnomalyAlert anomaly = anomaly("abstention");
        EvidenceContext emptyContext = EvidenceContext.build(anomaly, List.of());
        when(anomalyQueries.detectAnomalies()).thenReturn(List.of(anomaly));
        when(evidenceOrchestrator.gatherContext(any(), any(), any(), any())).thenReturn(emptyContext);

        JsonNode result = awaitTerminal(request(anomaly.id()));

        assertEquals("ABSTAINED", result.get("status").asText());
        assertEquals("NO_EVIDENCE_FOUND", result.get("abstentionReason").asText());
        verify(aiInterpreter, never()).interpret(any());
    }

    @Test
    void provider_failure_does_not_break_anomaly_access() throws Exception {
        AnomalyAlert anomaly = anomaly("failure");
        EvidenceContext context = EvidenceContext.build(anomaly, List.of(evidence("health-2", "digest")));
        when(anomalyQueries.detectAnomalies()).thenReturn(List.of(anomaly));
        when(evidenceOrchestrator.gatherContext(any(), any(), any(), any())).thenReturn(context);
        when(aiInterpreter.interpret(context)).thenThrow(new RuntimeException("provider unavailable"));

        JsonNode result = awaitTerminal(request(anomaly.id()));

        assertEquals("FAILED", result.get("status").asText());
        mockMvc.perform(get("/api/v1/dashboard/anomalies").with(user("owner")))
                .andExpect(status().isOk());
    }

    @Test
    void tenant_a_cannot_read_tenant_b_job() throws Exception {
        AnomalyInterpretationJob foreignJob = new AnomalyInterpretationJob(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "foreign-anomaly", "fingerprint", "context");
        repository.saveAndFlush(foreignJob);
        when(anomalyQueries.detectAnomalies()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/anomalies/interpretations/jobs/" + foreignJob.getId())
                        .with(user("owner")))
                .andExpect(status().isNotFound());
    }

    private UUID request(String anomalyId) throws Exception {
        MvcResult response = mockMvc.perform(post("/api/v1/anomalies/" + anomalyId + "/interpretations")
                        .with(user("owner"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(response.getResponse().getContentAsString()).get("jobId").asText());
    }

    private JsonNode awaitTerminal(UUID jobId) throws Exception {
        for (int attempt = 0; attempt < 50; attempt++) {
            JsonNode response = getJob(jobId);
            if (List.of("COMPLETED", "ABSTAINED", "FAILED", "STALE").contains(response.get("status").asText())) {
                return response;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Le job Copilot n’a pas atteint un état terminal.");
    }

    private JsonNode getJob(UUID jobId) throws Exception {
        MvcResult response = mockMvc.perform(get("/api/v1/anomalies/interpretations/jobs/" + jobId)
                        .with(user("owner")))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(response.getResponse().getContentAsString());
    }

    private AnomalyAlert anomaly(String suffix) {
        return new AnomalyAlert("anomaly-" + suffix, "DROP_MILK_PRODUCTION", "AVERTISSEMENT",
                "Chute de production", "Production en baisse de 12 kg", UUID.randomUUID(), OffsetDateTime.now());
    }

    private Evidence evidence(String id, String digest) {
        Evidence evidence = mock(Evidence.class);
        when(evidence.evidenceId()).thenReturn(id);
        when(evidence.contentDigest()).thenReturn(digest);
        when(evidence.sourceModule()).thenReturn("HEALTH");
        when(evidence.factType()).thenReturn("HEALTH_EVENT");
        when(evidence.observationDate()).thenReturn(OffsetDateTime.now().minusDays(1));
        when(evidence.values()).thenReturn(Map.of("diagnostic", "Observation vétérinaire"));
        when(evidence.supportLevel()).thenReturn(SupportLevel.STRONG);
        when(evidence.temporalRelation()).thenReturn(TemporalRelation.ANTE_ANOMALY);
        return evidence;
    }
}
