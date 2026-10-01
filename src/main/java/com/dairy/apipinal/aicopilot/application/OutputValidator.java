package com.dairy.apipinal.aicopilot.application;

import com.dairy.apipinal.aicopilot.api.Evidence;
import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class OutputValidator {

    private final ActionPolicyService actionPolicyService;

    public OutputValidator(ActionPolicyService actionPolicyService) {
        this.actionPolicyService = actionPolicyService;
    }

    public enum ValidationErrorType {
        NULL_OUTPUT,
        NO_HYPOTHESES,
        EMPTY_EVIDENCE_REFERENCES,
        UNKNOWN_EVIDENCE_ID,
        TEMPORAL_VIOLATION_POSTERIOR_EVIDENCE,
        FORBIDDEN_CAUSAL_CLAIM,
        NUMERICAL_FABRICATION,
        UNKNOWN_ACTION_CODE,
        INCOMPATIBLE_ACTION,
        INELIGIBLE_ACTION
    }

    public record ValidationError(ValidationErrorType type, String message) {}

    public record ValidationReport(
            boolean isValid,
            List<ValidationError> errors,
            LLMOutput sanitizedOutput
    ) {}

    private static final List<String> FORBIDDEN_CAUSAL_PHRASES = List.of(
            "a causé",
            "a entraîné",
            "est responsable de",
            "est dû à",
            "est due à",
            "a provoqué",
            "a généré"
    );

    private static final Pattern NUMERIC_PATTERN = Pattern.compile("[-+]?\\d+(?:[.,]\\d+)?\\s*(?:%|kg)?");

    public LLMOutput validate(LLMOutput rawOutput, EvidenceContext context) {
        if (rawOutput == null) {
            throw new IllegalArgumentException("Raw output is null");
        }
        ValidationReport report = validateDetailed(rawOutput, context);
        if (!report.isValid()) {
            String errorSummary = report.errors().stream()
                    .map(ValidationError::message)
                    .collect(Collectors.joining("; "));
            throw new IllegalStateException("LLM Output validation failed: " + errorSummary);
        }
        return report.sanitizedOutput();
    }

    public ValidationReport validateDetailed(LLMOutput rawOutput, EvidenceContext context) {
        List<ValidationError> errors = new ArrayList<>();

        if (rawOutput == null) {
            errors.add(new ValidationError(ValidationErrorType.NULL_OUTPUT, "Raw output is null"));
            return new ValidationReport(false, errors, null);
        }

        Map<String, Evidence> evidenceMap = context.evidences().stream()
                .collect(Collectors.toMap(Evidence::evidenceId, e -> e, (e1, e2) -> e1));

        Map<String, String> aggregatedEntityState = new HashMap<>();
        for (Evidence ev : context.evidences()) {
            if (ev.values() != null) {
                aggregatedEntityState.putAll(ev.values());
            }
        }

        // Validate Hypotheses
        List<LLMOutput.Hypothesis> validHypotheses = new ArrayList<>();
        if (rawOutput.hypotheses() == null || rawOutput.hypotheses().isEmpty()) {
            errors.add(new ValidationError(ValidationErrorType.NO_HYPOTHESES, "No hypotheses provided"));
        } else {
            for (LLMOutput.Hypothesis hypothesis : rawOutput.hypotheses()) {
                boolean hypothesisValid = true;

                if (hypothesis.referencedEvidenceIds() == null || hypothesis.referencedEvidenceIds().isEmpty()) {
                    errors.add(new ValidationError(ValidationErrorType.EMPTY_EVIDENCE_REFERENCES,
                            "Hypothesis has no referenced evidences: " + hypothesis.description()));
                    continue;
                }

                // Grounding: all evidence IDs must exist
                for (String refId : hypothesis.referencedEvidenceIds()) {
                    Evidence ev = evidenceMap.get(refId);
                    if (ev == null) {
                        errors.add(new ValidationError(ValidationErrorType.UNKNOWN_EVIDENCE_ID,
                                "Unknown evidence ID referenced: " + refId));
                        hypothesisValid = false;
                    } else {
                        // Temporal check: observationDate must not be strictly after anomaly dateDetection
                        if (context.anomaly() != null && context.anomaly().dateDetection() != null) {
                            if (ev.observationDate().isAfter(context.anomaly().dateDetection())) {
                                errors.add(new ValidationError(ValidationErrorType.TEMPORAL_VIOLATION_POSTERIOR_EVIDENCE,
                                        "Posterior evidence referenced (" + refId + " at " + ev.observationDate()
                                                + " after anomaly at " + context.anomaly().dateDetection() + ")"));
                                hypothesisValid = false;
                            }
                        }
                    }
                }

                // Prudence check: no forbidden causal claims
                String explanationLower = hypothesis.description() != null ? hypothesis.description().toLowerCase(Locale.ROOT) : "";
                for (String phrase : FORBIDDEN_CAUSAL_PHRASES) {
                    if (explanationLower.contains(phrase)) {
                        errors.add(new ValidationError(ValidationErrorType.FORBIDDEN_CAUSAL_CLAIM,
                                "Forbidden deterministic causal phrasing detected: '" + phrase + "' in: " + hypothesis.description()));
                        hypothesisValid = false;
                        break;
                    }
                }

                // Numerical fabrication check
                if (hypothesis.description() != null && !isNumericallyFaithful(hypothesis.description(), context)) {
                    errors.add(new ValidationError(ValidationErrorType.NUMERICAL_FABRICATION,
                            "Fabricated numerical values or percentages detected in: " + hypothesis.description()));
                    hypothesisValid = false;
                }

                if (hypothesisValid) {
                    validHypotheses.add(hypothesis);
                }
            }
        }

        // Validate Actions
        List<String> validActions = new ArrayList<>();
        if (rawOutput.suggestedActionCodes() != null) {
            String anomalyType = context.anomaly() != null ? context.anomaly().code() : "";
            for (String actionCode : rawOutput.suggestedActionCodes()) {
                ActionPolicyService.ActionPolicyEvaluation eval =
                        actionPolicyService.evaluate(actionCode, anomalyType, aggregatedEntityState);

                if (!eval.isValid()) {
                    ValidationErrorType errType = switch (eval.status()) {
                        case UNKNOWN_ACTION -> ValidationErrorType.UNKNOWN_ACTION_CODE;
                        case INCOMPATIBLE_ACTION -> ValidationErrorType.INCOMPATIBLE_ACTION;
                        case INELIGIBLE_ACTION -> ValidationErrorType.INELIGIBLE_ACTION;
                        default -> ValidationErrorType.UNKNOWN_ACTION_CODE;
                    };
                    errors.add(new ValidationError(errType, eval.reason()));
                } else {
                    validActions.add(actionCode);
                }
            }
        }

        boolean overallValid = !validHypotheses.isEmpty();

        LLMOutput sanitized = overallValid ? new LLMOutput(validHypotheses, validActions) : null;
        return new ValidationReport(overallValid, errors, sanitized);
    }

    private boolean isNumericallyFaithful(String text, EvidenceContext context) {
        // Collect all numeric tokens present in context evidences and anomaly
        Set<String> knownNumbers = new HashSet<>();
        if (context.anomaly() != null && context.anomaly().description() != null) {
            Matcher m = NUMERIC_PATTERN.matcher(context.anomaly().description());
            while (m.find()) {
                knownNumbers.add(normalizeNumeric(m.group()));
            }
        }
        for (Evidence ev : context.evidences()) {
            if (ev.values() != null) {
                for (String v : ev.values().values()) {
                    Matcher m = NUMERIC_PATTERN.matcher(v);
                    while (m.find()) {
                        knownNumbers.add(normalizeNumeric(m.group()));
                    }
                }
            }
        }

        // If explanation introduces numbers that do not match any known numbers or dates
        Matcher expMatcher = NUMERIC_PATTERN.matcher(text);
        while (expMatcher.find()) {
            String token = normalizeNumeric(expMatcher.group());
            // Ignore small integers (e.g. 1, 2) that could be list counters or day indicators
            if (token.equals("1") || token.equals("2") || token.equals("3")) {
                continue;
            }
            if (!knownNumbers.contains(token)) {
                return false; // Fabricated number detected!
            }
        }

        return true;
    }

    private String normalizeNumeric(String val) {
        return val.replace(" ", "").replace(",", ".").replace("%", "").replace("kg", "").trim();
    }
}
