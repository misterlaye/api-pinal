package com.dairy.apipinal.health.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record HealthDashboardSummary(
        Kpis kpis,
        List<Alert> alerts,
        List<HistoryEvent> history,
        List<Vaccination> vaccinations,
        ThermalStress thermalStress
) {
    public record Kpis(
            int alertsCount,
            String alertsSubtitle,
            String vaccinationsRatio,
            String vaccinationsSubtitle,
            String nextVisit,
            String nextVisitSubtitle,
            int healthScore,
            String healthScoreSubtitle
    ) {}

    public record Alert(
            UUID id,
            UUID animalId,
            String animalName,
            String animalIdentifier,
            String type,
            String severity,
            String date,
            String message
    ) {}

    public record HistoryEvent(
            UUID id,
            UUID animalId,
            String animalName,
            String date,
            String type,
            String description,
            String status
    ) {}

    public record Vaccination(
            UUID id,
            String name,
            String vaccine,
            String avatar,
            String date,
            boolean isUrgent
    ) {}

    public record ThermalStress(
            int averageTemp,
            String season,
            String location,
            String riskLevel,
            int riskScore,
            List<String> recommendations
    ) {}
}
