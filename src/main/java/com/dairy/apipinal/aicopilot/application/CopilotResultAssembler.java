package com.dairy.apipinal.aicopilot.application;

import com.dairy.apipinal.aicopilot.api.CopilotResult;
import com.dairy.apipinal.aicopilot.api.Evidence;
import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CopilotResultAssembler {

    private static final String LIMITATION =
            "Cette interprétation générée par l’IA rapproche des faits enregistrés dans Pinal. Elle ne constitue pas un diagnostic certain.";

    private final ActionPolicyService actionPolicyService;

    public CopilotResultAssembler(ActionPolicyService actionPolicyService) {
        this.actionPolicyService = actionPolicyService;
    }

    public CopilotResult assemble(LLMOutput output, EvidenceContext context) {
        Map<String, Evidence> evidencesById = context.evidences().stream()
                .collect(Collectors.toMap(Evidence::evidenceId, Function.identity()));

        var hypotheses = output.hypotheses().stream()
                .map(hypothesis -> new CopilotResult.Hypothesis(
                        hypothesis.description(),
                        hypothesis.referencedEvidenceIds().stream()
                                .map(evidencesById::get)
                                .filter(java.util.Objects::nonNull)
                                .map(this::toView)
                                .toList()
                ))
                .toList();

        var actions = output.suggestedActionCodes().stream()
                .map(code -> new CopilotResult.SuggestedAction(code, actionPolicyService.labelFor(code)))
                .toList();

        return new CopilotResult(hypotheses, actions, LIMITATION);
    }

    private CopilotResult.EvidenceView toView(Evidence evidence) {
        return new CopilotResult.EvidenceView(
                evidence.sourceModule(),
                evidence.factType(),
                evidence.observationDate(),
                evidence.values(),
                evidence.supportLevel().name()
        );
    }
}
