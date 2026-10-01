package com.dairy.apipinal.aicopilot.domain;

public record CopilotExecutionMetrics(
        long contextBuildDurationMs,
        long llmRequestDurationMs,
        long outputValidationDurationMs,
        long totalDurationMs,
        int promptTokens,
        int completionTokens,
        double estimatedCostUsd
) {
    public static CopilotExecutionMetrics compute(
            long contextBuildMs,
            long llmRequestMs,
            long validationMs,
            int promptTokens,
            int completionTokens,
            double costPer1kPromptTokens,
            double costPer1kCompletionTokens
    ) {
        long total = contextBuildMs + llmRequestMs + validationMs;
        double cost = (promptTokens / 1000.0 * costPer1kPromptTokens) + (completionTokens / 1000.0 * costPer1kCompletionTokens);
        return new CopilotExecutionMetrics(
                contextBuildMs,
                llmRequestMs,
                validationMs,
                total,
                promptTokens,
                completionTokens,
                Math.round(cost * 100000.0) / 100000.0
        );
    }
}
