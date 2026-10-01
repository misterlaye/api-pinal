package com.dairy.apipinal.aicopilot.infrastructure.gateway;

import com.dairy.apipinal.aicopilot.application.LLMOutput;
import com.dairy.apipinal.aicopilot.domain.CopilotExecutionMetrics;
import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Function;

@Component
public class LLMGatewayAdapter implements AIInterpreter {

    private final PromptBuilder promptBuilder;
    private final GeminiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private Function<EvidenceContext, LLMOutput> customProvider;
    private CopilotExecutionMetrics lastExecutionMetrics;
    private String modelVersionOverride;

    public LLMGatewayAdapter(
            PromptBuilder promptBuilder,
            GeminiProperties properties,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this(promptBuilder, properties, restClientBuilder.build(), objectMapper);
    }

    LLMGatewayAdapter(
            PromptBuilder promptBuilder,
            GeminiProperties properties,
            RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.promptBuilder = promptBuilder;
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public LLMGatewayAdapter() {
        this(new PromptBuilder(), new GeminiProperties(), RestClient.builder().build(),
                new ObjectMapper().findAndRegisterModules());
    }

    /**
     * Test-only convenience constructor. It still requires either an explicit
     * test provider or configured credentials before an interpretation can run.
     */
    public LLMGatewayAdapter(PromptBuilder promptBuilder) {
        this(promptBuilder, new GeminiProperties(), RestClient.builder().build(),
                new ObjectMapper().findAndRegisterModules());
    }

    public void setCustomProvider(Function<EvidenceContext, LLMOutput> provider) {
        this.customProvider = provider;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersionOverride = modelVersion;
    }

    public CopilotExecutionMetrics getLastExecutionMetrics() {
        return lastExecutionMetrics;
    }

    @Override
    public LLMOutput interpret(EvidenceContext context) {
        if (context.evidences().isEmpty()) {
            throw new IllegalArgumentException("Cannot interpret an empty evidence context");
        }

        long contextStartedAt = System.currentTimeMillis();
        String systemPrompt = promptBuilder.buildSystemPrompt(properties.getPromptVersion());
        String userPrompt = promptBuilder.buildUserPrompt(context, properties.getPromptVersion());
        long contextBuildMs = System.currentTimeMillis() - contextStartedAt;

        long requestStartedAt = System.currentTimeMillis();
        LLMOutput output = executeWithResilience(() -> customProvider != null
                ? customProvider.apply(context)
                : callGemini(systemPrompt, userPrompt));
        long requestMs = System.currentTimeMillis() - requestStartedAt;

        lastExecutionMetrics = CopilotExecutionMetrics.compute(
                contextBuildMs,
                requestMs,
                0,
                (systemPrompt.length() + userPrompt.length()) / 4,
                output.toString().length() / 4,
                0.00125,
                0.00500
        );
        return output;
    }

    private LLMOutput callGemini(String systemPrompt, String userPrompt) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("AI provider is not configured");
        }
        try {
            String response = restClient.post()
                    .uri(normalizeEndpoint(properties.getEndpoint())
                                    + "/models/{model}:generateContent?key={apiKey}",
                            properties.getModel(), properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestPayload(systemPrompt, userPrompt))
                    .retrieve()
                    .body(String.class);
            return parseResponse(response);
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("Gemini HTTP " + exception.getStatusCode().value(), exception);
        } catch (ResourceAccessException exception) {
            throw new IllegalStateException("Gemini request unavailable", exception);
        }
    }

    private String requestPayload(String systemPrompt, String userPrompt) {
        ObjectNode root = objectMapper.createObjectNode();
        root.putObject("system_instruction").putArray("parts").addObject().put("text", systemPrompt);
        root.putArray("contents").addObject()
                .put("role", "user")
                .putArray("parts").addObject().put("text", userPrompt);

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("temperature", 0.0);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.set("responseSchema", outputSchema());
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize Gemini request", exception);
        }
    }

    private ObjectNode outputSchema() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "OBJECT");
        ObjectNode propertiesNode = root.putObject("properties");
        ObjectNode hypotheses = propertiesNode.putObject("hypotheses");
        hypotheses.put("type", "ARRAY");
        ObjectNode hypothesis = hypotheses.putObject("items");
        hypothesis.put("type", "OBJECT");
        hypothesis.putArray("required").add("description").add("referencedEvidenceIds");
        ObjectNode hypothesisProperties = hypothesis.putObject("properties");
        hypothesisProperties.putObject("description").put("type", "STRING");
        hypothesisProperties.putObject("referencedEvidenceIds")
                .put("type", "ARRAY").putObject("items").put("type", "STRING");
        propertiesNode.putObject("suggestedActionCodes")
                .put("type", "ARRAY").putObject("items").put("type", "STRING");
        root.putArray("required").add("hypotheses").add("suggestedActionCodes");
        return root;
    }

    private LLMOutput parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (!text.isTextual() || text.asText().isBlank()) {
                throw new IllegalStateException("Gemini returned no structured output");
            }
            return objectMapper.readValue(text.asText(), LLMOutput.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Gemini returned malformed structured output", exception);
        }
    }

    private LLMOutput executeWithResilience(java.util.function.Supplier<LLMOutput> call) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException exception) {
                lastFailure = exception;
                if (!isRetryable(exception) || attempt == 3) {
                    throw exception;
                }
                try {
                    Thread.sleep(50L * (1L << attempt));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Gemini request interrupted", interrupted);
                }
            }
        }
        throw lastFailure;
    }

    private boolean isRetryable(RuntimeException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        return message.contains("429") || message.contains("503") || message.contains("unavailable");
    }

    private String normalizeEndpoint(String endpoint) {
        return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
    }

    @Override
    public String getModelVersion() {
        return modelVersionOverride != null ? modelVersionOverride : properties.getModel();
    }

    @Override
    public String getPromptVersion() {
        return properties.getPromptVersion();
    }

    @Override
    public String getOutputSchemaVersion() {
        return properties.getOutputSchemaVersion();
    }
}
