package com.dairy.apipinal.health.application;

import com.dairy.apipinal.health.api.HealthDashboardQueries;
import com.dairy.apipinal.health.api.HealthDashboardSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class HealthDashboardQueriesImpl implements HealthDashboardQueries {

    private final JdbcTemplate jdbcTemplate;

    public HealthDashboardQueriesImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public HealthDashboardSummary getDashboardSummary(UUID tenantId, UUID exploitationId) {
        Integer alertsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM evenement_sanitaire es " +
                "JOIN animal a ON a.id = es.animal_id " +
                "WHERE es.date_fin IS NULL AND a.exploitation_id = ? AND a.statut = 'ACTIF'",
                Integer.class, exploitationId);
        if (alertsCount == null) alertsCount = 0;

        HealthDashboardSummary.Kpis kpis = new HealthDashboardSummary.Kpis(
                alertsCount, alertsCount > 0 ? "Surveillance requise" : "Aucune alerte active",
                "10/12", "Rappels à planifier", // Hardcoded mock for now, missing vaccination table
                "5 jours", "Dr. Sarr - visite de routine", // Mock
                alertsCount > 0 ? 87 : 100, alertsCount > 0 ? "Bon état général" : "Troupeau en parfaite santé"
        );

        List<HealthDashboardSummary.Alert> alerts = jdbcTemplate.query(
                "SELECT es.id, a.id as animalId, a.nom as animalName, a.identifiant as animalIdentifier, " +
                "es.description as type, 'RISQUE ÉLEVÉ' as severity, es.date_heure as date, es.diagnostic as message " +
                "FROM evenement_sanitaire es " +
                "JOIN animal a ON a.id = es.animal_id " +
                "WHERE es.date_fin IS NULL AND a.exploitation_id = ? " +
                "ORDER BY es.date_heure DESC LIMIT 5",
                (rs, rowNum) -> new HealthDashboardSummary.Alert(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("animalId")),
                        rs.getString("animalName"),
                        rs.getString("animalIdentifier"),
                        rs.getString("type"),
                        rs.getString("severity"),
                        rs.getString("date"),
                        rs.getString("message") != null ? rs.getString("message") : rs.getString("type")
                ),
                exploitationId
        );

        List<HealthDashboardSummary.HistoryEvent> history = jdbcTemplate.query(
                "SELECT es.id, a.id as animalId, a.nom as animalName, " +
                "es.date_heure as date, 'Consultation' as type, es.description, " +
                "CASE WHEN es.date_fin IS NULL THEN 'EN COURS' ELSE 'TERMINÉ' END as status " +
                "FROM evenement_sanitaire es " +
                "JOIN animal a ON a.id = es.animal_id " +
                "WHERE a.exploitation_id = ? " +
                "ORDER BY es.date_heure DESC LIMIT 10",
                (rs, rowNum) -> new HealthDashboardSummary.HistoryEvent(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("animalId")),
                        rs.getString("animalName"),
                        rs.getString("date"),
                        rs.getString("type"),
                        rs.getString("description"),
                        rs.getString("status")
                ),
                exploitationId
        );

        List<HealthDashboardSummary.Vaccination> vaccinations = Collections.emptyList();

        HealthDashboardSummary.ThermalStress thermalStress = new HealthDashboardSummary.ThermalStress(
                31, "SAISON SÈCHE", "Thiès, Sénégal", "Modéré", 64,
                List.of("Assurer un accès permanent à l'ombre", "Augmenter la fréquence d'hydratation")
        );

        return new HealthDashboardSummary(kpis, alerts, history, vaccinations, thermalStress);
    }
}
