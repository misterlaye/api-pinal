package com.dairy.apipinal.aicopilot.application;

import java.util.List;

public record LLMOutput(
        List<Hypothesis> hypotheses,
        List<String> suggestedActionCodes
) {
    public record Hypothesis(
            String description,
            List<String> referencedEvidenceIds
    ) {}
}
