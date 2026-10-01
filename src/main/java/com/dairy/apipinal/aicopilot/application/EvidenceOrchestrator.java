package com.dairy.apipinal.aicopilot.application;

import com.dairy.apipinal.aicopilot.api.Evidence;
import com.dairy.apipinal.aicopilot.api.EvidenceProvider;
import com.dairy.apipinal.aicopilot.api.TemporalRelation;
import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvidenceOrchestrator {

    private final List<EvidenceProvider> providers;

    public EvidenceOrchestrator(List<EvidenceProvider> providers) {
        this.providers = providers;
    }

    public EvidenceContext gatherContext(UUID tenantId, UUID exploitationId, UUID animalId, AnomalyAlert anomaly) {
        List<Evidence> allEvidences = new ArrayList<>();

        for (EvidenceProvider provider : providers) {
            allEvidences.addAll(provider.gatherEvidences(tenantId, exploitationId, animalId, anomaly));
        }

        // The observation timestamp is authoritative: a provider cannot make
        // a later fact admissible by assigning a misleading relation.
        List<Evidence> admissibleEvidences = allEvidences.stream()
                .filter(e -> e.observationDate() != null)
                .filter(e -> !e.observationDate().isAfter(anomaly.dateDetection()))
                .filter(e -> e.temporalRelation() != TemporalRelation.POST_ANOMALY)
                .collect(Collectors.toList());

        return EvidenceContext.build(anomaly, admissibleEvidences);
    }
}
