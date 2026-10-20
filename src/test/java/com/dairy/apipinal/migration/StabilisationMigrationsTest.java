package com.dairy.apipinal.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie, sur des données antérieures à la stabilisation (schéma V26), que les migrations
 * V27 (authentification), V28 (hachage des PIN) et V29 (isolation par exploitation)
 * préservent et réattribuent correctement les données existantes.
 */
class StabilisationMigrationsTest {

    private static PostgreSQLContainer<?> postgres;
    private static JdbcTemplate jdbc;
    private static Flyway flyway;

    private static final UUID TENANT_1 = UUID.randomUUID();
    private static final UUID TENANT_2 = UUID.randomUUID();
    private static final UUID EXP_1 = UUID.randomUUID();
    private static final UUID EXP_2 = UUID.randomUUID();
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID WORKER = UUID.randomUUID();
    private static final UUID ANIMAL_1 = UUID.randomUUID();
    private static final UUID ANIMAL_2 = UUID.randomUUID();
    private static final UUID ALIMENT = UUID.randomUUID();
    private static final UUID RATION_1 = UUID.randomUUID();
    private static final UUID RATION_2 = UUID.randomUUID();
    private static final UUID CHARGE = UUID.randomUUID();

    @BeforeAll
    static void migrateLegacyData() {
        postgres = new PostgreSQLContainer<>("postgres:18");
        postgres.start();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbc = new JdbcTemplate(dataSource);

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("26").load().migrate();
        insertLegacyData();
        flyway = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
        flyway.migrate();
    }

    @AfterAll
    static void stop() {
        postgres.stop();
    }

    private static void insertLegacyData() {
        UUID race = jdbc.queryForObject("SELECT id FROM race LIMIT 1", UUID.class);
        jdbc.update("INSERT INTO tenant (id, nom, statut, created_at, updated_at, version) VALUES (?, 'T1', 'ACTIF', NOW(), NOW(), 0)", TENANT_1);
        jdbc.update("INSERT INTO tenant (id, nom, statut, created_at, updated_at, version) VALUES (?, 'T2', 'ACTIF', NOW(), NOW(), 0)", TENANT_2);
        jdbc.update("INSERT INTO exploitation (id, tenant_id, nom, localite, actif, created_at, updated_at, version) VALUES (?, ?, 'E1', 'Thiès', true, NOW(), NOW(), 0)", EXP_1, TENANT_1);
        jdbc.update("INSERT INTO exploitation (id, tenant_id, nom, localite, actif, created_at, updated_at, version) VALUES (?, ?, 'E2', 'Louga', true, NOW(), NOW(), 0)", EXP_2, TENANT_2);
        jdbc.update("INSERT INTO utilisateur (id, nom, prenom, statut, telephone, created_at, updated_at, version, code_pin) VALUES (?, 'Diallo', 'Awa', 'ACTIF', '+221700000001', NOW(), NOW(), 0, '111111')", OWNER);
        jdbc.update("INSERT INTO utilisateur (id, nom, prenom, statut, telephone, created_at, updated_at, version, code_pin) VALUES (?, 'Ba', 'Moussa', 'ACTIF', '+221700000002', NOW(), NOW(), 0, '246810')", WORKER);
        jdbc.update("INSERT INTO membership (id, utilisateur_id, exploitation_id, role, created_at, updated_at, version) VALUES (?, ?, ?, 'PROPRIETAIRE', NOW(), NOW(), 0)", UUID.randomUUID(), OWNER, EXP_1);
        jdbc.update("INSERT INTO membership (id, utilisateur_id, exploitation_id, role, created_at, updated_at, version) VALUES (?, ?, ?, 'EMPLOYE', NOW(), NOW(), 0)", UUID.randomUUID(), WORKER, EXP_1);
        jdbc.update("INSERT INTO refresh_token (id, token_hash, user_id, expires_at, revoked, created_at) VALUES (?, 'h', ?, NOW() + INTERVAL '1 day', false, NOW())", UUID.randomUUID(), WORKER);

        jdbc.update("INSERT INTO animal (id, tenant_id, exploitation_id, race_id, identifiant, nom, statut, sexe, created_at, updated_at, version) VALUES (?, ?, ?, ?, 'V1', 'Vache 1', 'ACTIF', 'FEMELLE', NOW(), NOW(), 0)", ANIMAL_1, TENANT_1, EXP_1, race);
        jdbc.update("INSERT INTO animal (id, tenant_id, exploitation_id, race_id, identifiant, nom, statut, sexe, created_at, updated_at, version) VALUES (?, ?, ?, ?, 'V2', 'Vache 2', 'ACTIF', 'FEMELLE', NOW(), NOW(), 0)", ANIMAL_2, TENANT_2, EXP_2, race);

        // Catalogue global (ancien modèle) utilisé par les deux exploitations
        jdbc.update("INSERT INTO aliment (id, code, nom, categorie, unite, actif, date_creation, date_modification, version) VALUES (?, 'FOIN', 'Foin', 'FOURRAGE', 'KG', true, NOW(), NOW(), 0)", ALIMENT);
        jdbc.update("INSERT INTO prix_aliment (id, aliment_id, prix_unitaire, date_debut, date_fin, version) VALUES (?, ?, 150, DATE '2026-01-01', NULL, 0)", UUID.randomUUID(), ALIMENT);

        // Ration 1 sans exploitation (antérieure à V15), ration 2 rattachée
        jdbc.update("INSERT INTO ration (id, tenant_id, exploitation_id, animal_id, date_debut, statut, origine, version) VALUES (?, ?, NULL, ?, DATE '2026-02-01', 'ACTIVE', 'ACTUELLE', 0)", RATION_1, TENANT_1, ANIMAL_1);
        jdbc.update("INSERT INTO ration (id, tenant_id, exploitation_id, animal_id, date_debut, statut, origine, version) VALUES (?, ?, ?, ?, DATE '2026-02-01', 'ACTIVE', 'ACTUELLE', 0)", RATION_2, TENANT_2, EXP_2, ANIMAL_2);
        jdbc.update("INSERT INTO ligne_ration (id, ration_id, aliment_id, quantite) VALUES (?, ?, ?, 8)", UUID.randomUUID(), RATION_1, ALIMENT);
        jdbc.update("INSERT INTO ligne_ration (id, ration_id, aliment_id, quantite) VALUES (?, ?, ?, 6)", UUID.randomUUID(), RATION_2, ALIMENT);

        jdbc.update("INSERT INTO charge_exploitation (id, tenant_id, exploitation_id, libelle, categorie, montant, date, version) VALUES (?, ?, NULL, 'Vétérinaire', 'SANTE', 5000, DATE '2026-02-10', 0)", CHARGE, TENANT_1);
    }

    @Test
    void allMigrationsAreApplied() {
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '23'", Integer.class)).isZero();
    }

    @Test
    void workerPinIsHashedAndOwnerPinIsRemoved() {
        String workerHash = jdbc.queryForObject("SELECT pin_hash FROM utilisateur WHERE id = ?", String.class, WORKER);
        assertThat(new BCryptPasswordEncoder().matches("246810", workerHash)).isTrue();
        assertThat(jdbc.queryForObject("SELECT pin_hash FROM utilisateur WHERE id = ?", String.class, OWNER)).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refresh_token WHERE revoked = false", Integer.class)).isZero();
    }

    @Test
    void legacyRowsAreAttachedToTheirExploitation() {
        assertThat(jdbc.queryForObject("SELECT exploitation_id FROM ration WHERE id = ?", UUID.class, RATION_1)).isEqualTo(EXP_1);
        assertThat(jdbc.queryForObject("SELECT exploitation_id FROM charge_exploitation WHERE id = ?", UUID.class, CHARGE)).isEqualTo(EXP_1);
    }

    @Test
    void globalCatalogueIsCopiedIntoEachExploitationWithItsPrices() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM aliment WHERE id = ?", Integer.class, ALIMENT)).isZero();
        List<Map<String, Object>> copies = jdbc.queryForList("SELECT id, exploitation_id, tenant_id FROM aliment WHERE code = 'FOIN'");
        assertThat(copies).hasSize(2);
        assertThat(copies).extracting(row -> row.get("exploitation_id")).containsExactlyInAnyOrder(EXP_1, EXP_2);

        for (Map<String, Object> copy : copies) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM prix_aliment WHERE aliment_id = ?", Integer.class, copy.get("id"))).isEqualTo(1);
        }
    }

    @Test
    void rationLinesPointToTheCopyOfTheirOwnExploitation() {
        UUID ligne1Aliment = jdbc.queryForObject("SELECT aliment_id FROM ligne_ration WHERE ration_id = ?", UUID.class, RATION_1);
        UUID ligne2Aliment = jdbc.queryForObject("SELECT aliment_id FROM ligne_ration WHERE ration_id = ?", UUID.class, RATION_2);
        assertThat(jdbc.queryForObject("SELECT exploitation_id FROM aliment WHERE id = ?", UUID.class, ligne1Aliment)).isEqualTo(EXP_1);
        assertThat(jdbc.queryForObject("SELECT exploitation_id FROM aliment WHERE id = ?", UUID.class, ligne2Aliment)).isEqualTo(EXP_2);
    }
}
