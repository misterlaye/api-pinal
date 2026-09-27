package com.dairy.apipinal.nutrition.api;

import java.util.List;
import java.util.UUID;

public record NutritionDashboardSummary(
        Kpis kpis,
        List<RationGroup> rationGroups,
        List<Stock> stocks,
        List<Delivery> deliveries
) {
    public record Kpis(
            double dailyCost,
            double dailyCostTrend,
            double averageRation,
            String averageRationLabel,
            double stockRemaining,
            String stockRemainingLabel,
            double efficiency,
            String efficiencyLabel
    ) {}

    public record RationGroup(
            UUID id,
            String name,
            String composition,
            double costPerHead,
            int animalCount,
            String status
    ) {}

    public record Stock(
            UUID id,
            String name,
            double quantity,
            String unit,
            String status,
            double consumptionRate
    ) {}

    public record Delivery(
            UUID id,
            String supplier,
            String product,
            double quantity,
            String unit,
            String date,
            String status
    ) {}
}
