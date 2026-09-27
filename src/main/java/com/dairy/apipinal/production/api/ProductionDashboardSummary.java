package com.dairy.apipinal.production.api;

import java.util.List;
import java.util.UUID;

public record ProductionDashboardSummary(
        Kpis kpis,
        ChartData chartData,
        List<AnimalProduction> animalProduction,
        List<HistoryEvent> history
) {
    public record Kpis(
            double totalProduction,
            double totalProductionTrend,
            double averagePerAnimal,
            String averagePerAnimalLabel,
            BestProducer bestProducer,
            double collectionRate,
            String collectionRateLabel
    ) {}

    public record BestProducer(
            String name,
            String avatar,
            String subtitle
    ) {}

    public record ChartData(
            List<String> labels,
            List<Double> currentWeek,
            List<Double> previousWeek
    ) {}

    public record AnimalProduction(
            UUID id,
            String name,
            String race,
            String avatar,
            double matin,
            double soir,
            double total,
            double trend,
            String status
    ) {}

    public record HistoryEvent(
            UUID id,
            String date,
            String session,
            double quantity,
            int cowsMilked,
            String operator
    ) {}
}
