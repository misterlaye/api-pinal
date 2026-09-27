package com.dairy.apipinal.production.application;

import com.dairy.apipinal.production.api.ProductionDashboardQueries;
import com.dairy.apipinal.production.api.ProductionDashboardSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ProductionDashboardQueriesImpl implements ProductionDashboardQueries {

    private final JdbcTemplate jdbcTemplate;

    public ProductionDashboardQueriesImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ProductionDashboardSummary getDashboardSummary(UUID tenantId, UUID exploitationId) {
        // Total production (7 days)
        Double totalProd = jdbcTemplate.queryForObject(
                "SELECT SUM(t.quantite_kg) FROM traite t " +
                "JOIN lactation l ON l.id = t.lactation_id " +
                "JOIN animal a ON a.id = l.animal_id " +
                "WHERE a.exploitation_id = ? AND t.date_heure >= CURRENT_DATE - INTERVAL '7 days'",
                Double.class, exploitationId
        );
        if (totalProd == null) totalProd = 0.0;

        // Average per animal
        Double avgPerAnimal = jdbcTemplate.queryForObject(
                "SELECT AVG(t.quantite_kg) FROM traite t " +
                "JOIN lactation l ON l.id = t.lactation_id " +
                "JOIN animal a ON a.id = l.animal_id " +
                "WHERE a.exploitation_id = ? AND t.date_heure >= CURRENT_DATE - INTERVAL '7 days'",
                Double.class, exploitationId
        );
        if (avgPerAnimal == null) avgPerAnimal = 0.0;

        // Best producer
        ProductionDashboardSummary.BestProducer bestProducer = null;
        try {
            bestProducer = jdbcTemplate.queryForObject(
                    "SELECT a.nom as name, SUM(t.quantite_kg) as total " +
                    "FROM animal a " +
                    "JOIN lactation l ON l.animal_id = a.id " +
                    "JOIN traite t ON t.lactation_id = l.id " +
                    "WHERE a.exploitation_id = ? AND t.date_heure >= CURRENT_DATE - INTERVAL '7 days' " +
                    "GROUP BY a.id, a.nom ORDER BY total DESC LIMIT 1",
                    (rs, rowNum) -> new ProductionDashboardSummary.BestProducer(
                            rs.getString("name"),
                            "https://loremflickr.com/150/150/cow?lock=1",
                            rs.getDouble("total") + " L/7j"
                    ),
                    exploitationId
            );
        } catch (Exception e) {
            bestProducer = new ProductionDashboardSummary.BestProducer("-", "https://loremflickr.com/150/150/cow?lock=1", "0 L");
        }

        ProductionDashboardSummary.Kpis kpis = new ProductionDashboardSummary.Kpis(
                totalProd, 0.0, avgPerAnimal, "Moyenne par traite",
                bestProducer,
                100, "Donnée réelle"
        );

        // Chart Data (Last 7 days)
        List<String> labels = new java.util.ArrayList<>();
        List<Double> currentWeek = new java.util.ArrayList<>();
        List<Double> previousWeek = new java.util.ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            final int daysAgo = i;
            String label = java.time.LocalDate.now().minusDays(i).format(java.time.format.DateTimeFormatter.ofPattern("EEE"));
            labels.add(label);

            Double dayTotal = jdbcTemplate.queryForObject(
                    "SELECT SUM(t.quantite_kg) FROM traite t " +
                    "JOIN lactation l ON l.id = t.lactation_id " +
                    "JOIN animal a ON a.id = l.animal_id " +
                    "WHERE a.exploitation_id = ? AND CAST(t.date_heure AS DATE) = CURRENT_DATE - INTERVAL '" + daysAgo + " days'",
                    Double.class, exploitationId
            );
            currentWeek.add(dayTotal != null ? dayTotal : 0.0);

            Double prevDayTotal = jdbcTemplate.queryForObject(
                    "SELECT SUM(t.quantite_kg) FROM traite t " +
                    "JOIN lactation l ON l.id = t.lactation_id " +
                    "JOIN animal a ON a.id = l.animal_id " +
                    "WHERE a.exploitation_id = ? AND CAST(t.date_heure AS DATE) = CURRENT_DATE - INTERVAL '" + (daysAgo + 7) + " days'",
                    Double.class, exploitationId
            );
            previousWeek.add(prevDayTotal != null ? prevDayTotal : 0.0);
        }

        ProductionDashboardSummary.ChartData chartData = new ProductionDashboardSummary.ChartData(
                labels, currentWeek, previousWeek
        );

        List<ProductionDashboardSummary.AnimalProduction> animalProduction = jdbcTemplate.query(
                "SELECT a.id, a.nom, 'Race Inconnue' as race, SUM(CASE WHEN t.type = 'MATIN' THEN t.quantite_kg ELSE 0 END) as matin, " +
                "SUM(CASE WHEN t.type = 'SOIR' THEN t.quantite_kg ELSE 0 END) as soir, " +
                "SUM(t.quantite_kg) as total " +
                "FROM animal a " +
                "JOIN lactation l ON l.animal_id = a.id " +
                "JOIN traite t ON t.lactation_id = l.id " +
                "WHERE a.exploitation_id = ? AND CAST(t.date_heure AS DATE) = CURRENT_DATE " +
                "GROUP BY a.id, a.nom ORDER BY total DESC",
                (rs, rowNum) -> new ProductionDashboardSummary.AnimalProduction(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("nom"),
                        rs.getString("race"),
                        "https://loremflickr.com/150/150/cow?lock=1",
                        rs.getDouble("matin"),
                        rs.getDouble("soir"),
                        rs.getDouble("total"),
                        0.0,
                        "NORMAL"
                ),
                exploitationId
        );

        List<ProductionDashboardSummary.HistoryEvent> history = jdbcTemplate.query(
                "SELECT t.id, t.date_heure, t.type, t.quantite_kg, 1 as cowsMilked " +
                "FROM traite t " +
                "JOIN lactation l ON l.id = t.lactation_id " +
                "JOIN animal a ON a.id = l.animal_id " +
                "WHERE a.exploitation_id = ? " +
                "ORDER BY t.date_heure DESC LIMIT 5",
                (rs, rowNum) -> new ProductionDashboardSummary.HistoryEvent(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("date_heure"),
                        rs.getString("type"),
                        rs.getDouble("quantite_kg"),
                        rs.getInt("cowsMilked"),
                        "Éleveur / Ouvrier"
                ),
                exploitationId
        );

        return new ProductionDashboardSummary(kpis, chartData, animalProduction, history);
    }
}
