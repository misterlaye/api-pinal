package com.dairy.apipinal.health.infrastructure.web;

import com.dairy.apipinal.health.api.HealthDashboardQueries;
import com.dairy.apipinal.health.api.HealthDashboardSummary;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthDashboardController {

    private final HealthDashboardQueries healthDashboardQueries;
    private final TenantContext tenantContext;

    public HealthDashboardController(HealthDashboardQueries healthDashboardQueries, TenantContext tenantContext) {
        this.healthDashboardQueries = healthDashboardQueries;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<HealthDashboardSummary> getDashboard() {
        return ResponseEntity.ok(
                healthDashboardQueries.getDashboardSummary(
                        tenantContext.currentTenantId(),
                        tenantContext.currentExploitationId()
                )
        );
    }
}
