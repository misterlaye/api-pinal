package com.dairy.apipinal.aicopilot.evaluation;

import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;

import java.util.List;

public record EvaluationCase(
        String caseId,
        String family,
        String description,
        AnomalyAlert anomaly,
        EvidenceContext evidenceContext,
        String expectedBehavior,
        List<String> expectedEvidenceIds,
        List<String> forbiddenEvidenceIds,
        List<String> expectedActionCodes,
        List<String> forbiddenActionCodes,
        ExpectedOutcome expectedOutcome,
        // Gold standard properties
        List<String> mustContain,
        List<String> mustNotContain,
        List<String> mustReference,
        List<String> mustNotReference,
        List<String> allowedActions,
        List<String> forbiddenActions
) {
    public enum ExpectedOutcome {
        SUCCESS,
        ABSTAINED,
        REJECTED,
        FAILED
    }
}
