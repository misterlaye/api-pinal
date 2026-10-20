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

        // Chart Data (Last 7 days + previous 7 days), totals per day in a single query
        java.util.Map<Integer, Double> totalsByDaysAgo = new java.util.HashMap<>();
        jdbcTemplate.query(
                "SELECT CURRENT_DATE - CAST(t.date_heure AS DATE) AS days_ago, SUM(t.quantite_litres) AS total FROM traite t " +
                "JOIN lactation l ON l.id = t.lactation_id " +
                "JOIN animal a ON a.id = l.animal_id " +
                "WHERE a.tenant_id = ? AND a.exploitation_id = ? " +
                "AND CAST(t.date_heure AS DATE) BETWEEN CURRENT_DATE - 13 AND CURRENT_DATE " +
                "GROUP BY 1",
                rs -> {
                    totalsByDaysAgo.put(rs.getInt("days_ago"), rs.getDouble("total"));
                },
                tenantId, exploitationId
        );

        List<String> labels = new java.util.ArrayList<>();
        List<Double> currentWeek = new java.util.ArrayList<>();
        List<Double> previousWeek = new java.util.ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            String label = java.time.LocalDate.now().minusDays(i).format(java.time.format.DateTimeFormatter.ofPattern("EEE"));
            labels.add(label);
            currentWeek.add(totalsByDaysAgo.getOrDefault(i, 0.0));
            previousWeek.add(totalsByDaysAgo.getOrDefault(i + 7, 0.0));
        }

        ProductionDashboardSummary.ChartData chartData = new ProductionDashboardSummary.ChartData(
                labels, currentWeek, previousWeek
        );

        // Total des 7 derniers jours civils et évolution vs les 7 jours précédents (mêmes données que le graphique)
        double totalProd = currentWeek.stream().mapToDouble(Double::doubleValue).sum();
        double previousTotal = previousWeek.stream().mapToDouble(Double::doubleValue).sum();
        Double totalTrend = previousTotal > 0
                ? Math.round(((totalProd - previousTotal) * 1000.0) / previousTotal) / 10.0
                : null;

        // Average per animal
        Double avgPerAnimal = jdbcTemplate.queryForObject(
                "SELECT AVG(t.quantite_litres) FROM traite t " +
                "JOIN lactation l ON l.id = t.lactation_id " +
                "JOIN animal a ON a.id = l.animal_id " +
                "WHERE a.tenant_id = ? AND a.exploitation_id = ? AND CAST(t.date_heure AS DATE) BETWEEN CURRENT_DATE - 6 AND CURRENT_DATE",
                Double.class, tenantId, exploitationId
        );
        if (avgPerAnimal == null) avgPerAnimal = 0.0;

        // Best producer
        ProductionDashboardSummary.BestProducer bestProducer = null;
        try {
            bestProducer = jdbcTemplate.queryForObject(
                    "SELECT a.nom as name, SUM(t.quantite_litres) as total " +
                    "FROM animal a " +
                    "JOIN lactation l ON l.animal_id = a.id " +
                    "JOIN traite t ON t.lactation_id = l.id " +
                    "WHERE a.tenant_id = ? AND a.exploitation_id = ? AND CAST(t.date_heure AS DATE) BETWEEN CURRENT_DATE - 6 AND CURRENT_DATE " +
                    "GROUP BY a.id, a.nom ORDER BY total DESC LIMIT 1",
                    (rs, rowNum) -> new ProductionDashboardSummary.BestProducer(
                            rs.getString("name"),
                            "../../../assets/images/default_cow.jpg",
                            rs.getDouble("total") + " L/7j"
                    ),
                    tenantId, exploitationId
            );
        } catch (Exception e) {
            bestProducer = new ProductionDashboardSummary.BestProducer("-", "../../../assets/images/default_cow.jpg", "0 L");
        }

        ProductionDashboardSummary.Kpis kpis = new ProductionDashboardSummary.Kpis(
                totalProd, totalTrend, avgPerAnimal, "Moyenne par traite (7 jours)",
                bestProducer
        );

        List<ProductionDashboardSummary.AnimalProduction> animalProduction = jdbcTemplate.query(
                "SELECT a.id, a.nom, r.libelle as race, SUM(CASE WHEN t.type = 'MATIN' THEN t.quantite_litres ELSE 0 END) as matin, " +
                "SUM(CASE WHEN t.type = 'SOIR' THEN t.quantite_litres ELSE 0 END) as soir, " +
                "SUM(t.quantite_litres) as total, " +
                "(SELECT SUM(t2.quantite_litres) FROM traite t2 JOIN lactation l2 ON l2.id = t2.lactation_id " +
                " WHERE l2.animal_id = a.id AND CAST(t2.date_heure AT TIME ZONE 'UTC' AS DATE) = CURRENT_DATE - 1) as hier " +
                "FROM animal a " +
                "JOIN race r ON a.race_id = r.id " +
                "JOIN lactation l ON l.animal_id = a.id " +
                "JOIN traite t ON t.lactation_id = l.id " +
                "WHERE a.tenant_id = ? AND a.exploitation_id = ? AND CAST(t.date_heure AT TIME ZONE 'UTC' AS DATE) = CURRENT_DATE " +
                "GROUP BY a.id, a.nom, r.libelle ORDER BY total DESC",
                (rs, rowNum) -> new ProductionDashboardSummary.AnimalProduction(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("nom"),
                        rs.getString("race"),
                        "../../../assets/images/default_cow.jpg",
                        rs.getDouble("matin"),
                        rs.getDouble("soir"),
                        rs.getDouble("total"),
                        tendance(rs.getDouble("total"), rs.getObject("hier") == null ? null : rs.getDouble("hier"))
                ),
                tenantId, exploitationId
        );

        List<ProductionDashboardSummary.HistoryEvent> history = jdbcTemplate.query(
                "SELECT t.id, t.date_heure, t.type, t.quantite_litres, 1 as cowsMilked, " +
                "COALESCE(u.prenom || ' ' || u.nom, 'Éleveur / Ouvrier') as worker " +
                "FROM traite t " +
                "JOIN lactation l ON l.id = t.lactation_id " +
                "JOIN animal a ON a.id = l.animal_id " +
                "LEFT JOIN utilisateur u ON t.auteur_id = u.id " +
                "WHERE a.tenant_id = ? AND a.exploitation_id = ? " +
                "ORDER BY t.date_heure DESC LIMIT 5",
                (rs, rowNum) -> new ProductionDashboardSummary.HistoryEvent(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("date_heure"),
                        rs.getString("type"),
                        rs.getDouble("quantite_litres"),
                        rs.getInt("cowsMilked"),
                        rs.getString("worker")
                ),
                tenantId, exploitationId
        );

        return new ProductionDashboardSummary(kpis, chartData, animalProduction, history);
    }

    /** Évolution (%) d'aujourd'hui par rapport à hier, {@code null} sans référence. */
    private static Double tendance(double aujourdhui, Double hier) {
        if (hier == null || hier <= 0) {
            return null;
        }
        return Math.round(((aujourdhui - hier) * 1000.0) / hier) / 10.0;
    }
}
