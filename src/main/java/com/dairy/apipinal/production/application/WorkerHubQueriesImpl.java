package com.dairy.apipinal.production.application;

import com.dairy.apipinal.production.api.WorkerHubQueries;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class WorkerHubQueriesImpl implements WorkerHubQueries {

    private final JdbcTemplate jdbcTemplate;

    public WorkerHubQueriesImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public WorkerHomeData getWorkerHomeData(UUID tenantId, UUID exploitationId) {
        // Mock data logic powered by SQL for the worker hub
        Integer totalAnimauxObj = jdbcTemplate.queryForObject(
                "SELECT COUNT(id) FROM animal WHERE exploitation_id = ? AND statut = 'ACTIF'",
                Integer.class, exploitationId
        );
        int totalAnimaux = totalAnimauxObj != null ? totalAnimauxObj : 0;

        int animauxASurveiller = 0; // Sera implémenté avec le module Santé

        Integer traitesMatin = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT a.id) FROM animal a " +
                "JOIN lactation l ON l.animal_id = a.id " +
                "JOIN traite t ON t.lactation_id = l.id " +
                "WHERE a.exploitation_id = ? AND CAST(t.date_heure AT TIME ZONE 'UTC' AS DATE) = CURRENT_DATE " +
                "AND t.type = 'MATIN'",
                Integer.class, exploitationId
        );
        if (traitesMatin == null) traitesMatin = 0;

        BigDecimal volMatinBd = jdbcTemplate.queryForObject(
                "SELECT SUM(t.quantite_kg) FROM animal a " +
                "JOIN lactation l ON l.animal_id = a.id " +
                "JOIN traite t ON t.lactation_id = l.id " +
                "WHERE a.exploitation_id = ? AND CAST(t.date_heure AT TIME ZONE 'UTC' AS DATE) = CURRENT_DATE " +
                "AND t.type = 'MATIN'",
                BigDecimal.class, exploitationId
        );
        double volMatin = volMatinBd != null ? volMatinBd.doubleValue() : 0.0;

        List<WorkerActivityData> recentActivities = jdbcTemplate.query(
                "SELECT t.id, 'Traite' as type, a.nom, a.identifiant, t.date_heure, t.quantite_kg " +
                "FROM traite t " +
                "JOIN lactation l ON l.id = t.lactation_id " +
                "JOIN animal a ON a.id = l.animal_id " +
                "WHERE a.exploitation_id = ? " +
                "ORDER BY t.date_heure DESC LIMIT 5",
                (rs, rowNum) -> {
                    OffsetDateTime dt = rs.getObject("date_heure", OffsetDateTime.class);
                    String timeStr = dt != null ? dt.format(DateTimeFormatter.ofPattern("HH:mm")) : "N/A";
                    return new WorkerActivityData(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("type"),
                        rs.getString("nom"),
                        rs.getString("identifiant"),
                        timeStr,
                        rs.getDouble("quantite_kg") + " L"
                    );
                },
                exploitationId
        );

        Integer totalLactationAnimaux = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT animal_id) FROM lactation WHERE date_fin IS NULL AND animal_id IN (SELECT id FROM animal WHERE exploitation_id = ?)",
                Integer.class, exploitationId
        );
        if (totalLactationAnimaux == null) totalLactationAnimaux = 0;

        return new WorkerHomeData(totalAnimaux, animauxASurveiller, traitesMatin, totalLactationAnimaux, volMatin, recentActivities);
    }

    @Override
    public List<WorkerAnimalData> getWorkerAnimals(UUID tenantId, UUID exploitationId) {
        return jdbcTemplate.query(
                "SELECT a.id, a.nom, a.identifiant, " +
                "(SELECT t.date_heure FROM traite t JOIN lactation l ON l.id = t.lactation_id WHERE l.animal_id = a.id ORDER BY t.date_heure DESC LIMIT 1) as last_milking_time, " +
                "(SELECT t.quantite_kg FROM traite t JOIN lactation l ON l.id = t.lactation_id WHERE l.animal_id = a.id ORDER BY t.date_heure DESC LIMIT 1) as last_milking_vol, " +
                "EXISTS (SELECT 1 FROM lactation l WHERE l.animal_id = a.id AND l.statut = 'EN_COURS') as is_lactating " +
                "FROM animal a " +
                "WHERE a.exploitation_id = ? AND a.statut = 'ACTIF'",
                (rs, rowNum) -> {
                    OffsetDateTime dt = rs.getObject("last_milking_time", OffsetDateTime.class);
                    String timeStr = dt != null ? dt.format(DateTimeFormatter.ofPattern("HH:mm")) : "-";
                    BigDecimal vol = rs.getObject("last_milking_vol", BigDecimal.class);
                    String volStr = vol != null ? vol.doubleValue() + " L" : "-";
                    boolean isLactating = rs.getBoolean("is_lactating");

                    // Simple heuristic for "done" today
                    boolean done = dt != null && dt.toLocalDate().equals(OffsetDateTime.now().toLocalDate());

                    return new WorkerAnimalData(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("nom"),
                        rs.getString("identifiant"),
                        "N/A", // La race nécessite une jointure inter-module, on omet ici ou on met N/A pour l'ouvrier
                        timeStr,
                        volStr,
                        !isLactating ? "Tarie" : (done ? "Traite faite" : "À traire"),
                        done,
                        isLactating
                    );
                },
                exploitationId
        );
    }

    @Override
    public WorkerAnimalDetailData getWorkerAnimalDetail(UUID tenantId, UUID animalId) {
        List<WorkerHistoryItem> history = jdbcTemplate.query(
                "SELECT t.id, t.date_heure, t.quantite_kg " +
                "FROM traite t " +
                "JOIN lactation l ON l.id = t.lactation_id " +
                "WHERE l.animal_id = ? " +
                "ORDER BY t.date_heure DESC LIMIT 5",
                (rs, rowNum) -> {
                    OffsetDateTime dt = rs.getObject("date_heure", OffsetDateTime.class);
                    return new WorkerHistoryItem(
                        UUID.fromString(rs.getString("id")),
                        dt != null ? dt.format(DateTimeFormatter.ofPattern("dd/MM")) : "-",
                        dt != null ? dt.format(DateTimeFormatter.ofPattern("HH:mm")) : "-",
                        rs.getDouble("quantite_kg") + " L"
                    );
                },
                animalId
        );

        String lastTime = history.isEmpty() ? "-" : history.get(0).time();
        String lastVol = history.isEmpty() ? "-" : history.get(0).volume();

        return jdbcTemplate.queryForObject(
                "SELECT id, nom, identifiant, " +
                "EXISTS (SELECT 1 FROM lactation l WHERE l.animal_id = animal.id AND l.statut = 'EN_COURS') as is_lactating " +
                "FROM animal WHERE id = ?",
                (rs, rowNum) -> new WorkerAnimalDetailData(
                        animalId,
                        rs.getString("nom"),
                        rs.getString("identifiant"),
                        "N/A",
                        rs.getBoolean("is_lactating") ? "En Lactation" : "Tarie",
                        "-", // Lot: Phase 4
                        "-", // Ration: Phase 4
                        lastTime,
                        lastVol,
                        history,
                        rs.getBoolean("is_lactating")
                ),
                animalId
        );
    }
}
