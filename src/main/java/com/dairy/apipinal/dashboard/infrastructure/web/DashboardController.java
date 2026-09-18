package com.dairy.apipinal.dashboard.infrastructure.web;

import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import com.dairy.apipinal.anomaly.api.AnomalyQueries;
import com.dairy.apipinal.dashboard.api.DashboardQueries;
import com.dairy.apipinal.dashboard.api.DashboardSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardQueries dashboardQueries;
    private final AnomalyQueries anomalyQueries;

    public DashboardController(
            DashboardQueries dashboardQueries,
            AnomalyQueries anomalyQueries
    ) {
        this.dashboardQueries = dashboardQueries;
        this.anomalyQueries = anomalyQueries;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> getSummary() {
        return ResponseEntity.ok(dashboardQueries.getDashboardSummary());
    }

    @GetMapping("/anomalies")
    public ResponseEntity<List<AnomalyAlert>> getAnomalies() {
        return ResponseEntity.ok(anomalyQueries.detectAnomalies());
    }
}
