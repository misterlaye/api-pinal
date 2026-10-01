package com.dairy.apipinal.aicopilot.infrastructure.gateway;

import com.dairy.apipinal.aicopilot.application.LLMOutput;
import com.dairy.apipinal.aicopilot.domain.EvidenceContext;

public interface AIInterpreter {
    
    LLMOutput interpret(EvidenceContext context);

    String getModelVersion();
    String getPromptVersion();
    String getOutputSchemaVersion();
}
