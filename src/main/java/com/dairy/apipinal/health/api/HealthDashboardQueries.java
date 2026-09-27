package com.dairy.apipinal.health.api;

import java.util.UUID;

public interface HealthDashboardQueries {
    HealthDashboardSummary getDashboardSummary(UUID tenantId, UUID exploitationId);
}
