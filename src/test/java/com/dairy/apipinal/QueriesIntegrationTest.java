package com.dairy.apipinal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Integration test to secure the critical SQL queries in the application.
 * Replaces the purely mock-based approach to ensure schema and syntax validity.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class QueriesIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void verifyDashboardQueriesSql() {
        UUID tenantId = UUID.randomUUID();
        UUID exploitationId = UUID.randomUUID();

        // 1. DashboardQueriesImpl - Vaches Taries
        assertDoesNotThrow(() -> {
            jdbcTemplate.queryForObject(
                "SELECT COUNT(id) FROM animal a WHERE statut = 'ACTIF' AND sexe = 'FEMELLE' AND tenant_id = ? AND exploitation_id = ? " +
                "AND EXISTS (SELECT 1 FROM lactation l WHERE l.animal_id = a.id) " +
                "AND NOT EXISTS (SELECT 1 FROM lactation l2 WHERE l2.animal_id = a.id AND l2.statut = 'EN_COURS')",
                Long.class, tenantId, exploitationId);
        });

        // 2. WorkerHubQueriesImpl - Animaux à surveiller
        assertDoesNotThrow(() -> {
            jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT a.id) FROM animal a " +
                "JOIN evenement_sanitaire es ON a.id = es.animal_id " +
                "WHERE a.tenant_id = ? AND a.exploitation_id = ? " +
                "AND a.statut = 'ACTIF' AND es.date_fin IS NULL",
                Integer.class, tenantId, exploitationId);
        });

        // 3. HealthDashboardQueriesImpl - Alerts Count
        assertDoesNotThrow(() -> {
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM evenement_sanitaire " +
                "WHERE tenant_id = ? AND exploitation_id = ? AND date_fin IS NULL",
                Integer.class, tenantId, exploitationId);
        });

        // 4. ProductionDashboardQueriesImpl - Total Milk
        assertDoesNotThrow(() -> {
            jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(quantite_kg), 0) FROM traite " +
                "WHERE tenant_id = ? AND exploitation_id = ? AND CAST(date_heure AS DATE) = CURRENT_DATE",
                Double.class, tenantId, exploitationId);
        });

        // 5. NutritionDashboardQueriesImpl - Active Rations
        assertDoesNotThrow(() -> {
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ration " +
                "WHERE tenant_id = ? AND exploitation_id = ? AND statut = 'ACTIVE' " +
                "AND (date_fin IS NULL OR date_fin >= CURRENT_DATE)",
                Long.class, tenantId, exploitationId);
        });
    }
}
