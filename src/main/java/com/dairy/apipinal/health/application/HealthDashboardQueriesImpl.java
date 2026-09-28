package com.dairy.apipinal.health.application;

import com.dairy.apipinal.health.api.HealthDashboardQueries;
import com.dairy.apipinal.health.api.HealthDashboardSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

        // 1. Alerts count (Active diseases, excluding vaccinations)
        Integer alertsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM evenement_sanitaire es " +
                "JOIN animal a ON a.id = es.animal_id " +
                "WHERE es.date_fin IS NULL AND a.tenant_id = ? AND a.exploitation_id = ? AND a.statut = 'ACTIF' " +
                "AND LOWER(es.description) NOT LIKE '%vaccin%'",
                Integer.class, tenantId, exploitationId);
        if (alertsCount == null) alertsCount = 0;

        // 2. Vaccinations à planifier (Rappels dans les 30 prochains jours)
        Integer rappelsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM evenement_sanitaire es " +
                "JOIN animal a ON a.id = es.animal_id " +
                "WHERE a.tenant_id = ? AND a.exploitation_id = ? AND a.statut = 'ACTIF' " +
                "AND LOWER(es.description) LIKE '%vaccin%' " +
                "AND es.date_fin IS NOT NULL " +
                "AND es.date_fin >= CURRENT_DATE " +
                "AND es.date_fin <= CURRENT_DATE + INTERVAL '30 days'",
                Integer.class, tenantId, exploitationId);
        if (rappelsCount == null) rappelsCount = 0;
        
        // Total animals
        Integer totalAnimals = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM animal WHERE tenant_id = ? AND exploitation_id = ? AND statut = 'ACTIF'",
                Integer.class, tenantId, exploitationId);
        if (totalAnimals == null) totalAnimals = 0;
        
        // Next visit (closest vaccination or checkup)
        String nextVisit = "Aucune visite planifiée";
        String nextVisitDesc = "Pas d'intervention urgente";
        List<java.sql.Date> upcomingDates = jdbcTemplate.queryForList(
                "SELECT es.date_fin FROM evenement_sanitaire es " +
                "JOIN animal a ON a.id = es.animal_id " +
                "WHERE a.tenant_id = ? AND a.exploitation_id = ? AND es.date_fin >= CURRENT_DATE " +
                "ORDER BY es.date_fin ASC LIMIT 1",
                java.sql.Date.class, tenantId, exploitationId);
        
        if (!upcomingDates.isEmpty()) {
            LocalDate next = upcomingDates.get(0).toLocalDate();
            long days = ChronoUnit.DAYS.between(LocalDate.now(), next);
            if (days == 0) {
                nextVisit = "Aujourd'hui";
            } else if (days == 1) {
                nextVisit = "Demain";
            } else {
                nextVisit = "Dans " + days + " jours";
            }
            nextVisitDesc = "Rappel(s) vaccinal(aux) prévu(s)";
        }

        HealthDashboardSummary.Kpis kpis = new HealthDashboardSummary.Kpis(
                alertsCount, alertsCount > 0 ? "Surveillance requise" : "Aucune alerte active",
                String.valueOf(rappelsCount), "Rappels à planifier (30j)",
                nextVisit, nextVisitDesc,
                totalAnimals > 0 ? (int)(((totalAnimals - alertsCount) / (double)totalAnimals) * 100) : 100, 
                alertsCount > 0 ? "Bon état général" : "Troupeau en parfaite santé"
        );

        // Alerts list
        List<HealthDashboardSummary.Alert> alerts = jdbcTemplate.query(
                "SELECT es.id, a.id as animalId, a.nom as animalName, a.identifiant as animalIdentifier, " +
                "es.description as type, 'RISQUE ÉLEVÉ' as severity, es.date_heure as date, es.diagnostic as message " +
                "FROM evenement_sanitaire es " +
                "JOIN animal a ON a.id = es.animal_id " +
                "WHERE es.date_fin IS NULL AND a.tenant_id = ? AND a.exploitation_id = ? " +
                "AND LOWER(es.description) NOT LIKE '%vaccin%' " +
                "ORDER BY es.date_heure DESC LIMIT 5",
                (rs, rowNum) -> new HealthDashboardSummary.Alert(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("animalId")),
                        rs.getString("animalName"),
                        rs.getString("animalIdentifier"),
                        rs.getString("type"),
                        rs.getString("severity"),
                        rs.getString("date"),
                        rs.getString("message") != null && !rs.getString("message").isBlank() ? rs.getString("message") : rs.getString("type")
                ),
                tenantId, exploitationId
        );

        // History
        List<HealthDashboardSummary.HistoryEvent> history = jdbcTemplate.query(
                "SELECT es.id, a.id as animalId, a.nom as animalName, " +
                "es.date_heure as date, " +
                "CASE WHEN LOWER(es.description) LIKE '%vaccin%' THEN 'Vaccination' ELSE 'Consultation' END as type, " +
                "es.description, " +
                "CASE WHEN es.date_fin IS NULL AND LOWER(es.description) NOT LIKE '%vaccin%' THEN 'EN COURS' ELSE 'TERMINÉ' END as status " +
                "FROM evenement_sanitaire es " +
                "JOIN animal a ON a.id = es.animal_id " +
                "WHERE a.tenant_id = ? AND a.exploitation_id = ? " +
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
                tenantId, exploitationId
        );

        // Vaccinations
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<HealthDashboardSummary.Vaccination> vaccinations = jdbcTemplate.query(
                "SELECT es.id, a.nom as animalName, es.traitement as vaccine, es.date_fin as date " +
                "FROM evenement_sanitaire es " +
                "JOIN animal a ON a.id = es.animal_id " +
                "WHERE a.tenant_id = ? AND a.exploitation_id = ? " +
                "AND LOWER(es.description) LIKE '%vaccin%' " +
                "AND es.date_fin >= CURRENT_DATE " +
                "ORDER BY es.date_fin ASC LIMIT 10",
                (rs, rowNum) -> {
                    LocalDate date = rs.getDate("date").toLocalDate();
                    boolean isUrgent = ChronoUnit.DAYS.between(LocalDate.now(), date) <= 15;
                    return new HealthDashboardSummary.Vaccination(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("animalName"),
                            rs.getString("vaccine") != null ? rs.getString("vaccine") : "Vaccin",
                            "/avatars/cow-default.jpg", // default avatar
                            date.format(fmt),
                            isUrgent
                    );
                },
                tenantId, exploitationId
        );

        // Thermal Stress (Dynamic based on month/location)
        String localite = "Sénégal";
        try {
            String dbLoc = jdbcTemplate.queryForObject("SELECT localite FROM exploitation WHERE tenant_id = ? AND id = ?", String.class, tenantId, exploitationId);
            if (dbLoc != null && !dbLoc.isBlank()) localite = dbLoc;
        } catch (Exception ignored) {}

        HealthDashboardSummary.ThermalStress thermalStress = null;

        return new HealthDashboardSummary(kpis, alerts, history, vaccinations, thermalStress);
    }
}
