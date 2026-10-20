package com.dairy.apipinal.nutrition;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Coûts alimentaires : catalogue propre à chaque exploitation, prix appliqués selon leur période de validité,
 * prix manquant signalé (jamais compté comme nul), cohérence entre Nutrition, Finance et rentabilité animale.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class NutritionCostIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtTokenProvider jwt;

    private ApiFixtures fermeA;
    private ApiFixtures fermeB;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        fermeA = new ApiFixtures(context, jdbc, jwt);
        fermeB = new ApiFixtures(context, jdbc, jwt);
    }

    @Test
    @DisplayName("Chaque exploitation a son catalogue et ses prix, même pour un code identique")
    void cataloguesAreIndependent() throws Exception {
        UUID foinA = aliment(fermeA, "FOIN", "Foin A");
        UUID foinB = aliment(fermeB, "FOIN", "Foin B");
        prix(fermeA, foinA, 100, today.minusDays(30));
        prix(fermeB, foinB, 250, today.minusDays(30));
        UUID vacheA = vacheAvecRation(fermeA, "A-1", today.minusDays(5), Map.of(foinA, 4));

        JsonNode dashA = dashboard(fermeA);
        JsonNode dashB = dashboard(fermeB);

        assertThat(dashA.at("/kpis/coutJournalier").decimalValue()).isEqualByComparingTo("400");   // 4 kg × 100, prix de A
        assertThat(dashA.at("/kpis/alimentsAuCatalogue").asLong()).isEqualTo(1);
        assertThat(dashB.at("/kpis/alimentsAuCatalogue").asLong()).isEqualTo(1);
        assertThat(dashB.get("rationGroups")).isEmpty();
        assertThat(dashB.get("consommations").toString()).doesNotContain(foinA.toString());
        // B ne peut pas composer une ration avec l'aliment de A.
        UUID vacheB = fermeB.createAnimal("B-1", "FEMELLE");
        UUID rationB = fermeB.id(fermeB.owner(post("/api/v1/animals/" + vacheB + "/rations"), Map.of(
                "dateDebut", today.toString(), "origine", "ACTUELLE")).andExpect(status().isCreated()));
        fermeB.owner(post("/api/v1/animals/" + vacheB + "/rations/" + rationB + "/lines"), Map.of("alimentId", foinA, "quantite", 1))
                .andExpect(status().isBadRequest());
        assertThat(vacheA).isNotNull();
    }

    @Test
    @DisplayName("Prix manquant : coût signalé incomplet, jamais nul, sans bloquer les autres indicateurs")
    void missingPriceIsNeverZero() throws Exception {
        UUID foin = aliment(fermeA, "FOIN", "Foin");
        UUID sel = aliment(fermeA, "SEL", "Pierre à sel");
        prix(fermeA, foin, 100, today.minusDays(30));
        vacheAvecRation(fermeA, "M-1", today, Map.of(foin, 3, sel, 1));

        JsonNode dash = dashboard(fermeA);
        assertThat(dash.at("/kpis/coutJournalierComplet").asBoolean()).isFalse();
        assertThat(dash.at("/kpis/coutJournalier").isNull()).isTrue();
        assertThat(dash.at("/kpis/coutJournalierConnu").decimalValue()).isEqualByComparingTo("300");
        assertThat(dash.at("/rationGroups/0/coutParJour").isNull()).isTrue();
        assertThat(dash.at("/rationGroups/0/alimentsSansPrix/0").asText()).isEqualTo("Pierre à sel");
        assertThat(dash.at("/alimentsSansPrix/0/name").asText()).isEqualTo("Pierre à sel");
        assertThat(dash.at("/kpis/couverture").decimalValue()).isEqualByComparingTo("100.0");   // autre indicateur disponible

        JsonNode mois = fermeA.body(fermeA.owner(get("/api/v1/finance/dashboard"), null).andExpect(status().isOk())).get("moisEnCours");
        assertThat(mois.get("coutAlimentation").isNull()).isTrue();
        assertThat(mois.get("coutAlimentationConnu").decimalValue()).isEqualByComparingTo("300");
        assertThat(mois.get("avertissements").toString()).contains("Pierre à sel");
        assertThat(mois.get("autresCharges").decimalValue()).isZero();                         // reste calculé
    }

    @Test
    @DisplayName("Changement de prix en cours de période : chaque jour au prix en vigueur, mêmes montants partout")
    void priceChangeIsAppliedDayByDay() throws Exception {
        LocalDate debutRation = today.minusDays(9);
        LocalDate changement = today.minusDays(4);
        UUID foin = aliment(fermeA, "FOIN", "Foin");
        prix(fermeA, foin, 100, today.minusDays(30));
        prix(fermeA, foin, 150, changement);                         // clôt automatiquement le prix précédent
        UUID vache = vacheAvecRation(fermeA, "P-1", debutRation, Map.of(foin, 2));

        // Nutrition : coût du jour au nouveau prix
        assertThat(dashboard(fermeA).at("/kpis/coutJournalier").decimalValue()).isEqualByComparingTo("300");

        // Rentabilité animale sur la période de la ration : 5 jours × 200 + 5 jours × 300
        JsonNode animal = fermeA.body(fermeA.owner(get("/api/v1/finance/animals/" + vache + "/rentabilite")
                .param("dateDebut", debutRation.toString()).param("dateFin", today.toString()), null)
                .andExpect(status().isOk()));
        assertThat(animal.get("coutAlimentation").decimalValue()).isEqualByComparingTo("2500");

        // Finance (mois en cours) : même règle, restreinte aux jours du mois
        BigDecimal attendu = BigDecimal.ZERO;
        for (LocalDate jour = YearMonth.from(today).atDay(1); !jour.isAfter(today); jour = jour.plusDays(1)) {
            if (jour.isBefore(debutRation)) continue;
            attendu = attendu.add(BigDecimal.valueOf(jour.isBefore(changement) ? 200 : 300));
        }
        JsonNode mois = fermeA.body(fermeA.owner(get("/api/v1/finance/dashboard"), null).andExpect(status().isOk())).get("moisEnCours");
        assertThat(mois.get("coutAlimentation").decimalValue()).isEqualByComparingTo(attendu);
    }

    private JsonNode dashboard(ApiFixtures ferme) throws Exception {
        return ferme.body(ferme.owner(get("/api/v1/nutrition/dashboard"), null).andExpect(status().isOk()));
    }

    private UUID aliment(ApiFixtures ferme, String code, String nom) throws Exception {
        return ferme.id(ferme.owner(post("/api/v1/aliments"), Map.of(
                "code", code, "nom", nom, "categorie", "FOURRAGE", "unite", "KG")).andExpect(status().isCreated()));
    }

    private void prix(ApiFixtures ferme, UUID aliment, int prix, LocalDate debut) throws Exception {
        ferme.owner(post("/api/v1/aliments/" + aliment + "/prices"), Map.of("prixUnitaire", prix, "dateDebut", debut.toString()))
                .andExpect(status().isOk());
    }

    private UUID vacheAvecRation(ApiFixtures ferme, String identifiant, LocalDate debut, Map<UUID, Integer> lignes) throws Exception {
        UUID vache = ferme.createAnimal(identifiant, "FEMELLE");
        UUID ration = ferme.id(ferme.owner(post("/api/v1/animals/" + vache + "/rations"), Map.of(
                "dateDebut", debut.toString(), "origine", "ACTUELLE")).andExpect(status().isCreated()));
        for (Map.Entry<UUID, Integer> ligne : lignes.entrySet()) {
            ferme.owner(post("/api/v1/animals/" + vache + "/rations/" + ration + "/lines"), Map.of(
                    "alimentId", ligne.getKey(), "quantite", ligne.getValue())).andExpect(status().isOk());
        }
        ferme.owner(post("/api/v1/animals/" + vache + "/rations/" + ration + "/activate"), null).andExpect(status().isOk());
        return vache;
    }
}
