package com.dairy.apipinal.aicopilot.domain;

import com.dairy.apipinal.aicopilot.api.Evidence;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public record EvidenceContext(
        AnomalyAlert anomaly,
        List<Evidence> evidences,
        String contextHash
) {
    public static EvidenceContext build(AnomalyAlert anomaly, List<Evidence> evidences) {
        String hash = computeContextHash(anomaly, evidences);
        return new EvidenceContext(anomaly, evidences, hash);
    }

    private static String computeContextHash(AnomalyAlert anomaly, List<Evidence> evidences) {
        String evidenceRepresentation = evidences.stream()
                .sorted(Comparator.comparing(Evidence::evidenceId))
                .map(e -> e.evidenceId() + ":" + e.contentDigest())
                .collect(Collectors.joining("|"));
        String canonicalRepresentation = String.join("|",
                anomaly.id(), anomaly.code(), String.valueOf(anomaly.animalId()),
                anomaly.titre(), anomaly.description(),
                HashUtils.formatDateTimeCanonical(anomaly.dateDetection()),
                evidenceRepresentation);
        return HashUtils.sha256(canonicalRepresentation);
    }
}
