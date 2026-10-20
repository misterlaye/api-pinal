package com.dairy.apipinal.health;

import com.dairy.apipinal.TestcontainersConfiguration;
import com.dairy.apipinal.identity.infrastructure.security.JwtTokenProvider;
import com.dairy.apipinal.support.ApiFixtures;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Indicateurs santé : convention vaccination explicite, animaux sortis exclus, aucune sévérité inventée.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class HealthDashboardIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtTokenProvider jwt;

    @Test
    void dashboardFollowsDocumentedRules() throws Exception {
        ApiFixtures api = new ApiFixtures(context, jdbc, jwt);
        UUID vache1 = api.createAnimal("S-1", "FEMELLE");
        UUID vache2 = api.createAnimal("S-2", "FEMELLE");
        UUID vacheSortie = api.createAnimal("S-3", "FEMELLE");
        api.createAnimal("S-4", "FEMELLE");

        // Signalement d'ouvrier mentionnant « vaccin » : reste un signalement ouvert.
        event(api, vache1, "Réaction au point de vaccin", null);
        event(api, vache1, "Toux", null);
        // Vaccination avec rappel dans 10 jours (convention du formulaire)
        event(api, vache2, "Vaccination", LocalDate.now().plusDays(10));
        // Signalement ouvert sur un animal ensuite sorti du troupeau : exclu des indicateurs.
        event(api, vacheSortie, "Boiterie", null);
        api.owner(post("/api/v1/animals/" + vacheSortie + "/sortie"), Map.of("motif", "VENTE")).andExpect(status().isOk());

        JsonNode dash = api.body(api.owner(get("/api/v1/health/dashboard"), null).andExpect(status().isOk()));

        assertThat(dash.at("/kpis/alertsCount").asInt()).isEqualTo(2);
        assertThat(dash.get("alerts")).hasSize(2);
        assertThat(dash.get("alerts").toString()).doesNotContain("Boiterie").doesNotContain("RISQUE");
        assertThat(dash.at("/alerts/0/severity").asText()).isEqualTo("OUVERT");
        assertThat(dash.at("/kpis/vaccinationsRatio").asText()).isEqualTo("1");
        assertThat(dash.get("vaccinations")).hasSize(1);
        assertThat(dash.at("/kpis/nextVisit").asText()).isEqualTo("Dans 10 jours");
        // 3 animaux actifs, 1 avec signalement ouvert -> 67 %
        assertThat(dash.at("/kpis/healthScore").asInt()).isEqualTo(67);
        assertThat(dash.has("thermalStress")).isFalse();
    }

    private void event(ApiFixtures api, UUID animal, String description, LocalDate dateFin) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("dateHeure", OffsetDateTime.now().minusHours(2).toString());
        body.put("description", description);
        if (dateFin != null) {
            body.put("dateFin", dateFin.toString());
        }
        api.owner(post("/api/v1/animals/" + animal + "/health-events"), body).andExpect(status().isCreated());
    }
}
