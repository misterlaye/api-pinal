package com.dairy.apipinal.aicopilot.application;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class ActionPolicyService {

    public enum ActionStatus {
        VALID,
        UNKNOWN_ACTION,
        INCOMPATIBLE_ACTION,
        INELIGIBLE_ACTION
    }

    public record ActionPolicyEvaluation(
            String actionCode,
            ActionStatus status,
            String reason
    ) {
        public boolean isValid() {
            return status == ActionStatus.VALID;
        }
    }

    private static final Set<String> KNOWN_ACTIONS = Set.of(
            "REVIEW_RATION",
            "VET_CONSULT",
            "MONITOR_NEXT_MILKING",
            "ISOLATE_ANIMAL",
            "CHECK_WATER_SUPPLY"
    );

    private static final Map<String, String> ACTION_LABELS = Map.of(
            "REVIEW_RATION", "Vérifier la ration",
            "VET_CONSULT", "Consulter un vétérinaire",
            "MONITOR_NEXT_MILKING", "Surveiller la prochaine traite",
            "ISOLATE_ANIMAL", "Évaluer la nécessité d’isoler l’animal",
            "CHECK_WATER_SUPPLY", "Vérifier l’approvisionnement en eau"
    );

    // Compatibility matrix: actionCode -> allowed anomaly types
    private static final Map<String, Set<String>> COMPATIBILITY_RULES = Map.of(
            "REVIEW_RATION", Set.of("DROP_MILK_PRODUCTION", "NUTRITION_DEFICIT", "ACIDOSIS_RISK"),
            "VET_CONSULT", Set.of("DROP_MILK_PRODUCTION", "MASTITIS_ALERT", "HEALTH_ANOMALY", "FEVER_ALERT"),
            "MONITOR_NEXT_MILKING", Set.of("DROP_MILK_PRODUCTION", "MILK_QUALITY_ANOMALY", "LOW_CONDUCTIVITY"),
            "ISOLATE_ANIMAL", Set.of("MASTITIS_ALERT", "FEVER_ALERT", "CONTAGIOUS_SYMPTOMS"),
            "CHECK_WATER_SUPPLY", Set.of("DROP_MILK_PRODUCTION", "HEAT_STRESS", "DEHYDRATION_RISK")
    );

    public boolean isKnownAction(String actionCode) {
        return actionCode != null && KNOWN_ACTIONS.contains(actionCode);
    }

    public String labelFor(String actionCode) {
        String label = ACTION_LABELS.get(actionCode);
        if (label == null) {
            throw new IllegalArgumentException("Code d'action inconnu : " + actionCode);
        }
        return label;
    }

    public boolean isCompatible(String actionCode, String anomalyType) {
        if (!isKnownAction(actionCode)) {
            return false;
        }
        Set<String> allowed = COMPATIBILITY_RULES.get(actionCode);
        return allowed != null && (allowed.contains(anomalyType) || allowed.contains("*"));
    }

    public boolean isEligible(String actionCode, Map<String, String> entityState) {
        if (!isKnownAction(actionCode)) {
            return false;
        }
        if (entityState == null || entityState.isEmpty()) {
            return true; // Default eligible if no restricting state
        }

        // Check specific entity eligibility constraints
        if ("ISOLATE_ANIMAL".equals(actionCode)) {
            String isolationStatus = entityState.getOrDefault("isolation_status", "NORMAL");
            if ("ALREADY_ISOLATED".equalsIgnoreCase(isolationStatus) || "ISOLATED".equalsIgnoreCase(isolationStatus)) {
                return false; // Animal is already isolated
            }
            String lifecycleStatus = entityState.getOrDefault("lifecycle_status", "ACTIVE");
            if ("DEAD".equalsIgnoreCase(lifecycleStatus) || "SOLD".equalsIgnoreCase(lifecycleStatus)) {
                return false;
            }
        }

        if ("REVIEW_RATION".equals(actionCode)) {
            String hasRation = entityState.getOrDefault("has_active_ration", "true");
            if ("false".equalsIgnoreCase(hasRation)) {
                return false; // Animal has no ration assigned
            }
        }

        if ("MONITOR_NEXT_MILKING".equals(actionCode)) {
            String lactationStatus = entityState.getOrDefault("is_lactating", "true");
            if ("false".equalsIgnoreCase(lactationStatus)) {
                return false; // Non-lactating animal cannot be monitored at milking
            }
        }

        return true;
    }

    public ActionPolicyEvaluation evaluate(String actionCode, String anomalyType, Map<String, String> entityState) {
        if (!isKnownAction(actionCode)) {
            return new ActionPolicyEvaluation(actionCode, ActionStatus.UNKNOWN_ACTION, "Unknown action code: " + actionCode);
        }
        if (!isCompatible(actionCode, anomalyType)) {
            return new ActionPolicyEvaluation(actionCode, ActionStatus.INCOMPATIBLE_ACTION,
                    "Action " + actionCode + " is not compatible with anomaly type " + anomalyType);
        }
        if (!isEligible(actionCode, entityState)) {
            return new ActionPolicyEvaluation(actionCode, ActionStatus.INELIGIBLE_ACTION,
                    "Action " + actionCode + " is ineligible given entity state: " + entityState);
        }
        return new ActionPolicyEvaluation(actionCode, ActionStatus.VALID, "Action is valid and eligible");
    }
}
