package com.dairy.apipinal.nutrition.api;

import java.util.UUID;

public interface NutritionDashboardQueries {
    NutritionDashboardSummary getDashboardSummary(UUID tenantId, UUID exploitationId);
}
