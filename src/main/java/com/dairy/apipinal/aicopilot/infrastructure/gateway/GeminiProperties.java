package com.dairy.apipinal.aicopilot.infrastructure.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pinal.ai-copilot.llm")
public class GeminiProperties {

    private String apiKey = "";
    private String endpoint = "https://generativelanguage.googleapis.com/v1beta";
    private String model = "gemini-1.5-pro";
    private String promptVersion = "v1";
    private String outputSchemaVersion = "v1";

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public String getOutputSchemaVersion() { return outputSchemaVersion; }
    public void setOutputSchemaVersion(String outputSchemaVersion) { this.outputSchemaVersion = outputSchemaVersion; }

    boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
