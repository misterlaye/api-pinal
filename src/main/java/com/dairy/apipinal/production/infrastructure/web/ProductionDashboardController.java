package com.dairy.apipinal.production.infrastructure.web;

import com.dairy.apipinal.production.api.ProductionDashboardQueries;
import com.dairy.apipinal.production.api.ProductionDashboardSummary;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/production")
@org.springframework.security.access.prepost.PreAuthorize("@authz.isOwner()")
public class ProductionDashboardController {

    private final ProductionDashboardQueries productionDashboardQueries;
    private final TenantContext tenantContext;

    public ProductionDashboardController(ProductionDashboardQueries productionDashboardQueries, TenantContext tenantContext) {
        this.productionDashboardQueries = productionDashboardQueries;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ProductionDashboardSummary> getDashboard() {
        return ResponseEntity.ok(
                productionDashboardQueries.getDashboardSummary(
                        tenantContext.currentTenantId(),
                        tenantContext.currentExploitationId()
                )
        );
    }
}
