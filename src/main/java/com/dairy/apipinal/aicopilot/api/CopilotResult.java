package com.dairy.apipinal.aicopilot.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record CopilotResult(
        List<Hypothesis> hypotheses,
        List<SuggestedAction> suggestedActions,
        String limitation
) {
    public record Hypothesis(String description, List<EvidenceView> evidences) {}

    public record EvidenceView(
            String domain,
            String factType,
            OffsetDateTime observationDate,
            Map<String, String> values,
            String supportLevel
    ) {}

    public record SuggestedAction(String code, String label) {}
}
