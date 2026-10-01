package com.dairy.apipinal.aicopilot.application;

import java.util.Set;

public class ActionCatalog {
    
    private static final Set<String> ALLOWED_ACTIONS = Set.of(
            "REVIEW_RATION",
            "VET_CONSULT",
            "MONITOR_NEXT_MILKING",
            "ISOLATE_ANIMAL",
            "CHECK_WATER_SUPPLY"
    );

    public static boolean isValidAction(String actionCode, String anomalyType) {
        if (!ALLOWED_ACTIONS.contains(actionCode)) {
            return false;
        }
        
        // Example of compatibility logic:
        // If anomalyType is NUTRITION_ISSUE, maybe ISOLATE_ANIMAL is not compatible.
        // For V1, we'll just check existence and a basic mapping.
        return true;
    }
}
