package com.dairy.apipinal.nutrition.infrastructure.web;

import com.dairy.apipinal.nutrition.api.NutritionDashboardQueries;
import com.dairy.apipinal.nutrition.api.NutritionDashboardSummary;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/nutrition")
public class NutritionDashboardController {

    private final NutritionDashboardQueries nutritionDashboardQueries;
    private final TenantContext tenantContext;

    public NutritionDashboardController(NutritionDashboardQueries nutritionDashboardQueries, TenantContext tenantContext) {
        this.nutritionDashboardQueries = nutritionDashboardQueries;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<NutritionDashboardSummary> getDashboard() {
        return ResponseEntity.ok(
                nutritionDashboardQueries.getDashboardSummary(
                        tenantContext.currentTenantId(),
                        tenantContext.currentExploitationId()
                )
        );
    }
}
