package com.dairy.apipinal.production.api;

import java.util.UUID;

public interface ProductionDashboardQueries {
    ProductionDashboardSummary getDashboardSummary(UUID tenantId, UUID exploitationId);
}
