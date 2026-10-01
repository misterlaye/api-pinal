package com.dairy.apipinal.aicopilot.domain;

import java.util.UUID;

public record AnalysisFingerprint(String value) {
    
    public static AnalysisFingerprint compute(
            UUID tenantId,
            String anomalyId,
            String contextHash,
            String rulesVersion,
            String promptVersion,
            String modelVersion,
            String outputSchemaVersion
    ) {
        String input = String.format("%s|%s|%s|%s|%s|%s|%s",
                tenantId.toString(),
                anomalyId,
                contextHash,
                rulesVersion,
                promptVersion,
                modelVersion,
                outputSchemaVersion
        );
        return new AnalysisFingerprint(HashUtils.sha256(input));
    }
}
