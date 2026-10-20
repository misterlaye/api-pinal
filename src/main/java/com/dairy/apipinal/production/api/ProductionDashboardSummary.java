package com.dairy.apipinal.production.api;

import java.util.List;
import java.util.UUID;

public record ProductionDashboardSummary(
        Kpis kpis,
        ChartData chartData,
        List<AnimalProduction> animalProduction,
        List<HistoryEvent> history
) {
    /**
     * @param totalProduction      litres des 7 derniers jours civils (aujourd'hui inclus)
     * @param totalProductionTrend évolution (%) par rapport aux 7 jours précédents, {@code null} sans référence
     */
    public record Kpis(
            double totalProduction,
            Double totalProductionTrend,
            double averagePerAnimal,
            String averagePerAnimalLabel,
            BestProducer bestProducer
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
            Double trend
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
