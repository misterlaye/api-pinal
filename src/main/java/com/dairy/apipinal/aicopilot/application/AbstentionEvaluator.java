package com.dairy.apipinal.aicopilot.application;

import com.dairy.apipinal.aicopilot.api.Evidence;
import com.dairy.apipinal.aicopilot.api.SupportLevel;
import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AbstentionEvaluator {

    public enum AbstentionReason {
        NONE,
        NO_EVIDENCE_FOUND,
        LOW_SIGNAL
    }

    public AbstentionReason evaluate(EvidenceContext context) {
        List<Evidence> evidences = context.evidences();

        if (evidences == null || evidences.isEmpty()) {
            return AbstentionReason.NO_EVIDENCE_FOUND;
        }

        boolean hasStrongOrModerate = evidences.stream()
                .anyMatch(e -> e.supportLevel() == SupportLevel.STRONG || e.supportLevel() == SupportLevel.MODERATE);

        if (!hasStrongOrModerate) {
            return AbstentionReason.LOW_SIGNAL;
        }

        return AbstentionReason.NONE;
    }
}
