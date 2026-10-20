package com.dairy.apipinal.finance;

import com.dairy.apipinal.TestcontainersConfiguration;
import com.dairy.apipinal.identity.infrastructure.security.JwtTokenProvider;
import com.dairy.apipinal.support.ApiFixtures;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lait en litres de la saisie aux indicateurs, et règles financières sur PostgreSQL réel.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MilkAndFinanceIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtTokenProvider jwt;

    private ApiFixtures api;
    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        api = new ApiFixtures(context, jdbc, jwt);
    }

    @Test
    @DisplayName("Saisie : une traite MATIN par vache et par jour, pas de date future")
    void milkingRules() throws Exception {
        UUID cow = lactatingCow("T-001");

        LocalDate hier = today.minusDays(1);
        milking(cow, "MATIN", 12, hier.atTime(6, 0)).andExpect(status().isCreated());
        milking(cow, "MATIN", 12, hier.atTime(6, 5)).andExpect(status().isConflict());
        milking(cow, "SOIR", 9, hier.atTime(18, 0)).andExpect(status().isCreated());
        milking(cow, "MATIN", 9, today.plusDays(2).atTime(6, 0)).andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT SUM(quantite_litres) FROM traite t JOIN lactation l ON l.id = t.lactation_id WHERE l.animal_id = ?",
                java.math.BigDecimal.class, cow)).isEqualByComparingTo("21");
    }

    @Test
    @DisplayName("Saisie par lot : lignes valides enregistrées, rejets motivés, jamais d'abandon silencieux")
    void batchReportsRejections() throws Exception {
        UUID cow1 = lactatingCow("B-001");
        UUID cow2 = lactatingCow("B-002");
        UUID dry = api.createAnimal("B-003", "FEMELLE");
        milking(cow2, "MATIN", 5, today.minusDays(1).atTime(6, 0)).andExpect(status().isCreated());

        JsonNode body = api.body(api.owner(post("/api/v1/milkings/batch"), Map.of(
                "dateHeure", today.minusDays(1).atTime(6, 30).atOffset(ZoneOffset.UTC).toString(),
                "type", "MATIN",
                "items", List.of(
                        Map.of("animalId", cow1, "quantiteLitres", 11),
                        Map.of("animalId", cow2, "quantiteLitres", 7),
                        Map.of("animalId", dry, "quantiteLitres", 4))
        )).andExpect(status().isCreated()));

        assertThat(body.get("enregistrees")).hasSize(1);
        assertThat(body.get("rejets")).hasSize(2);
        assertThat(body.get("rejets").toString()).contains("déjà enregistrée").contains("Aucune lactation en cours");

        api.owner(post("/api/v1/milkings/batch"), Map.of(
                "type", "MATIN",
                "items", List.of(Map.of("animalId", dry, "quantiteLitres", 4))
        )).andExpect(status().is(422));
    }

    @Test
    @DisplayName("Finance : les litres saisis sont valorisés tels quels, mêmes chiffres sur les deux tableaux de bord")
    void financeUsesLitresAndIsConsistentAcrossDashboards() throws Exception {
        UUID cow = lactatingCow("F-001");
        api.owner(post("/api/v1/finance/prix-vente"), Map.of("prixParLitre", 500, "dateDebut", today.minusDays(40).toString()))
                .andExpect(status().isCreated());
        milking(cow, "MATIN", 10, maintenant()).andExpect(status().isCreated());

        UUID aliment = api.id(api.owner(post("/api/v1/aliments"), Map.of(
                "code", "CONC", "nom", "Concentré", "categorie", "CONCENTRE", "unite", "KG")).andExpect(status().isCreated()));
        api.owner(post("/api/v1/aliments/" + aliment + "/prices"), Map.of("prixUnitaire", 100, "dateDebut", today.minusDays(60).toString()))
                .andExpect(status().isOk());
        UUID ration = api.id(api.owner(post("/api/v1/animals/" + cow + "/rations"), Map.of(
                "dateDebut", today.toString(), "origine", "ACTUELLE")).andExpect(status().isCreated()));
        api.owner(post("/api/v1/animals/" + cow + "/rations/" + ration + "/lines"), Map.of("alimentId", aliment, "quantite", 3))
                .andExpect(status().isOk());
        api.owner(post("/api/v1/animals/" + cow + "/rations/" + ration + "/activate"), null).andExpect(status().isOk());
        api.owner(post("/api/v1/finance/charges"), Map.of(
                "libelle", "Vétérinaire", "categorie", "SANTE", "montant", 1000, "date", today.toString()))
                .andExpect(status().isCreated());
        api.owner(post("/api/v1/finance/charges"), Map.of(
                "libelle", "Achat concentré", "categorie", "ALIMENTATION", "montant", 99999, "date", today.toString()))
                .andExpect(status().isCreated());

        JsonNode mois = api.body(api.owner(get("/api/v1/finance/dashboard"), null).andExpect(status().isOk())).get("moisEnCours");
        assertThat(mois.get("volumeLitres").decimalValue()).isEqualByComparingTo("10");          // pas de conversion kg -> L
        assertThat(mois.get("chiffreAffaires").decimalValue()).isEqualByComparingTo("5000");
        assertThat(mois.get("coutAlimentation").decimalValue()).isEqualByComparingTo("300");     // 3 kg × 100, 1 jour
        assertThat(mois.get("autresCharges").decimalValue()).isEqualByComparingTo("1000");      // l'achat ALIMENTATION n'est pas recompté
        assertThat(mois.get("marge").decimalValue()).isEqualByComparingTo("3700");
        assertThat(mois.get("avertissements")).isEmpty();

        JsonNode resume = api.body(api.owner(get("/api/v1/dashboard/summary"), null).andExpect(status().isOk()));
        assertThat(resume.at("/production/litresAujourdhui").decimalValue()).isEqualByComparingTo("10");
        assertThat(resume.at("/finance/derniers30Jours/chiffreAffaires").decimalValue()).isEqualByComparingTo("5000");
        assertThat(resume.at("/finance/derniers30Jours/marge").decimalValue()).isEqualByComparingTo("3700");
    }

    @Test
    @DisplayName("Finance : un prix manquant donne un avertissement et des valeurs inconnues, sans bloquer l'écran")
    void missingPricesAreExplicit() throws Exception {
        UUID cow = lactatingCow("P-001");
        milking(cow, "MATIN", 10, maintenant()).andExpect(status().isCreated());
        UUID aliment = api.id(api.owner(post("/api/v1/aliments"), Map.of(
                "code", "FOIN", "nom", "Foin", "categorie", "FOURRAGE", "unite", "KG")).andExpect(status().isCreated()));
        UUID ration = api.id(api.owner(post("/api/v1/animals/" + cow + "/rations"), Map.of(
                "dateDebut", today.toString(), "origine", "ACTUELLE")).andExpect(status().isCreated()));
        api.owner(post("/api/v1/animals/" + cow + "/rations/" + ration + "/lines"), Map.of("alimentId", aliment, "quantite", 3))
                .andExpect(status().isOk());
        api.owner(post("/api/v1/animals/" + cow + "/rations/" + ration + "/activate"), null).andExpect(status().isOk());

        JsonNode mois = api.body(api.owner(get("/api/v1/finance/dashboard"), null).andExpect(status().isOk())).get("moisEnCours");
        assertThat(mois.get("volumeLitres").decimalValue()).isEqualByComparingTo("10");
        assertThat(mois.get("chiffreAffaires").isNull()).isTrue();
        assertThat(mois.get("coutAlimentation").isNull()).isTrue();
        assertThat(mois.get("marge").isNull()).isTrue();
        assertThat(mois.get("avertissements").toString()).contains("PRIX_LAIT_MANQUANT").contains("PRIX_ALIMENT_MANQUANT").contains("Foin");

        api.owner(get("/api/v1/dashboard/summary"), null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Prix : un nouveau prix clôt le prix en cours la veille (lait et aliments)")
    void newPriceClosesCurrentPrice() throws Exception {
        api.owner(post("/api/v1/finance/prix-vente"), Map.of("prixParLitre", 500, "dateDebut", today.minusDays(30).toString()))
                .andExpect(status().isCreated());
        api.owner(post("/api/v1/finance/prix-vente"), Map.of("prixParLitre", 550, "dateDebut", today.minusDays(5).toString()))
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT date_fin FROM prix_vente_lait WHERE exploitation_id = ? AND prix_par_litre = 500",
                LocalDate.class, api.exploitationId)).isEqualTo(today.minusDays(6));

        UUID aliment = api.id(api.owner(post("/api/v1/aliments"), Map.of(
                "code", "SON", "nom", "Son de blé", "categorie", "CONCENTRE", "unite", "KG")).andExpect(status().isCreated()));
        api.owner(post("/api/v1/aliments/" + aliment + "/prices"), Map.of("prixUnitaire", 100, "dateDebut", today.minusDays(30).toString()))
                .andExpect(status().isOk());
        api.owner(post("/api/v1/aliments/" + aliment + "/prices"), Map.of("prixUnitaire", 120, "dateDebut", today.minusDays(2).toString()))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT date_fin FROM prix_aliment WHERE aliment_id = ? AND prix_unitaire = 100",
                LocalDate.class, aliment)).isEqualTo(today.minusDays(3));
        // Un prix antérieur au prix en cours chevauche l'historique : refusé.
        api.owner(post("/api/v1/aliments/" + aliment + "/prices"), Map.of("prixUnitaire", 90, "dateDebut", today.minusDays(10).toString()))
                .andExpect(status().isConflict());
    }

    /** Instant de traite d'aujourd'hui (UTC) déjà passé, quelle que soit l'heure d'exécution du test. */
    private java.time.LocalDateTime maintenant() {
        return java.time.LocalDateTime.now(ZoneOffset.UTC).minusSeconds(30);
    }

    private UUID lactatingCow(String identifiant) throws Exception {
        UUID cow = api.createAnimal(identifiant, "FEMELLE");
        api.owner(post("/api/v1/lactations"), Map.of("animalId", cow, "dateDebut", today.minusDays(10).toString()))
                .andExpect(status().isCreated());
        return cow;
    }

    private org.springframework.test.web.servlet.ResultActions milking(UUID cow, String type, double litres, java.time.LocalDateTime when)
            throws Exception {
        return api.owner(post("/api/v1/animals/" + cow + "/milkings"), Map.of(
                "dateHeure", OffsetDateTime.of(when, ZoneOffset.UTC).toString(), "type", type, "quantiteLitres", litres));
    }
}
