package com.dairy.apipinal.lifecycle;

import com.dairy.apipinal.TestcontainersConfiguration;
import com.dairy.apipinal.identity.infrastructure.security.JwtTokenProvider;
import com.dairy.apipinal.support.ApiFixtures;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cohérence des parcours métier entre modules, par HTTP, sur PostgreSQL réel.
 * Les effets d'une opération (sortie, vêlage, avortement) sont vérifiés immédiatement, sans attente :
 * ils sont appliqués dans la même transaction.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BusinessConsistencyIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtTokenProvider jwt;

    private ApiFixtures api;

    @BeforeEach
    void setUp() {
        api = new ApiFixtures(context, jdbc, jwt);
    }

    @Test
    @DisplayName("Sortie par changement de statut = même effet que la déclaration de sortie, sans attente")
    void statusChangeGoesThroughExitWithAllEffects() throws Exception {
        UUID cow = api.createAnimal("S-001", "FEMELLE");
        startLactation(cow, LocalDate.now().minusDays(20));
        UUID ration = activeRation(cow);
        UUID cycle = inseminate(cow, LocalDate.now().minusDays(30));

        api.owner(patch("/api/v1/animals/" + cow + "/status?status=VENDU"), null).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT statut FROM animal WHERE id = ?", String.class, cow)).isEqualTo("VENDU");
        assertThat(jdbc.queryForObject("SELECT motif FROM mouvement_sortie WHERE animal_id = ?", String.class, cow)).isEqualTo("VENTE");
        assertThat(jdbc.queryForObject("SELECT statut FROM lactation WHERE animal_id = ?", String.class, cow)).isEqualTo("TERMINEE");
        assertThat(jdbc.queryForObject("SELECT statut FROM ration WHERE id = ?", String.class, ration)).isEqualTo("TERMINEE");
        assertThat(jdbc.queryForObject("SELECT statut FROM cycle_reproduction WHERE id = ?", String.class, cycle)).isEqualTo("VIDE");
    }

    @Test
    @DisplayName("Une sortie est définitive et ses données sont validées")
    void exitIsFinalAndValidated() throws Exception {
        UUID cow = api.createAnimal("S-002", "FEMELLE");

        api.owner(post("/api/v1/animals/" + cow + "/sortie"), Map.of("motif", "VOL", "dateSortie", LocalDate.now().toString()))
                .andExpect(status().isBadRequest());
        api.owner(post("/api/v1/animals/" + cow + "/sortie"), Map.of("motif", "DECES", "dateSortie", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT statut FROM animal WHERE id = ?", String.class, cow)).isEqualTo("ACTIF");

        api.owner(post("/api/v1/animals/" + cow + "/sortie"), Map.of("motif", "DECES", "dateSortie", LocalDate.now().toString()))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT statut FROM animal WHERE id = ?", String.class, cow)).isEqualTo("DECEDE");

        api.owner(patch("/api/v1/animals/" + cow + "/status?status=ACTIF"), null).andExpect(status().isConflict());
        api.owner(post("/api/v1/animals/" + cow + "/sortie"), Map.of("motif", "VENTE")).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM mouvement_sortie WHERE animal_id = ?", Integer.class, cow)).isEqualTo(1);
    }

    @Test
    @DisplayName("Vêlage : lactation et veaux créés ensemble ; un veau invalide annule tout")
    void calvingIsAtomic() throws Exception {
        UUID cow = api.createAnimal("V-001", "FEMELLE");
        api.createAnimal("VEAU-EXISTANT", "MALE");
        UUID cycle = inseminate(cow, LocalDate.now().minusDays(290));
        constat(cycle, "POSITIF").andExpect(status().isOk());

        api.owner(post("/api/v1/reproduction/cycles/" + cycle + "/velage"), Map.of(
                "dateReelle", LocalDate.now().toString(),
                "veaux", List.of(Map.of("identifiant", "VEAU-EXISTANT", "sexe", "MALE", "indexPortee", 1))
        )).andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT statut FROM cycle_reproduction WHERE id = ?", String.class, cycle)).isEqualTo("GESTANTE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lactation WHERE animal_id = ?", Integer.class, cow)).isZero();

        api.owner(post("/api/v1/reproduction/cycles/" + cycle + "/velage"), Map.of(
                "dateReelle", LocalDate.now().toString(),
                "veaux", List.of(Map.of("identifiant", "VEAU-NOUVEAU", "sexe", "FEMELLE", "indexPortee", 1))
        )).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT statut FROM cycle_reproduction WHERE id = ?", String.class, cycle)).isEqualTo("TERMINEE_VELAGE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lactation WHERE animal_id = ? AND statut = 'EN_COURS' AND cycle_reproduction_id = ?",
                Integer.class, cow, cycle)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT mere_id FROM animal WHERE identifiant = 'VEAU-NOUVEAU' AND exploitation_id = ?",
                UUID.class, api.exploitationId)).isEqualTo(cow);
    }

    @Test
    @DisplayName("Avortement : événement sanitaire créé immédiatement avec l'auteur réel")
    void abortionCreatesHealthEventSynchronously() throws Exception {
        UUID cow = api.createAnimal("A-001", "FEMELLE");
        UUID cycle = inseminate(cow, LocalDate.now().minusDays(100));
        constat(cycle, "POSITIF").andExpect(status().isOk());

        api.owner(post("/api/v1/reproduction/cycles/" + cycle + "/avortement"), null).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT created_by FROM evenement_sanitaire WHERE source_cycle_id = ?", UUID.class, cycle))
                .isEqualTo(api.ownerId);
    }

    @Test
    @DisplayName("Reproduction : résultat de constat et dates contrôlés")
    void reproductionFactsAreValidated() throws Exception {
        UUID cow = api.createAnimal("R-001", "FEMELLE");
        UUID cycle = inseminate(cow, LocalDate.now().minusDays(40));

        constat(cycle, "PEUT-ETRE").andExpect(status().isBadRequest());
        api.owner(post("/api/v1/reproduction/cycles/" + cycle + "/constat"), Map.of(
                "dateConstat", LocalDate.now().minusDays(60).toString(), "resultat", "POSITIF"
        )).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT statut FROM cycle_reproduction WHERE id = ?", String.class, cycle)).isEqualTo("EN_ATTENTE_CONSTAT");

        constat(cycle, "POSITIF").andExpect(status().isOk());
        api.owner(post("/api/v1/reproduction/cycles/" + cycle + "/velage"), Map.of(
                "dateReelle", LocalDate.now().minusDays(50).toString(), "veaux", List.of()
        )).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Lactation et traite : pas de lactation pour un mâle, pas de lactation implicite")
    void lactationRules() throws Exception {
        UUID bull = api.createAnimal("M-001", "MALE");
        api.owner(post("/api/v1/lactations"), Map.of("animalId", bull, "dateDebut", LocalDate.now().toString()))
                .andExpect(status().isConflict());

        UUID cow = api.createAnimal("L-001", "FEMELLE");
        api.owner(post("/api/v1/animals/" + cow + "/milkings"), Map.of(
                "dateHeure", OffsetDateTime.now().toString(), "type", "MATIN", "quantiteLitres", 8
        )).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lactation WHERE animal_id = ?", Integer.class, cow)).isZero();
    }

    @Test
    @DisplayName("Suppression : impossible si l'animal a un historique, possible sinon")
    void deletionRespectsHistory() throws Exception {
        UUID withHistory = api.createAnimal("D-001", "FEMELLE");
        startLactation(withHistory, LocalDate.now().minusDays(3));
        api.owner(delete("/api/v1/animals/" + withHistory), null).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM animal WHERE id = ?", Integer.class, withHistory)).isEqualTo(1);

        UUID withoutHistory = api.createAnimal("D-002", "FEMELLE");
        api.owner(delete("/api/v1/animals/" + withoutHistory), null).andExpect(status().isNoContent());
    }

    private void startLactation(UUID cow, LocalDate debut) throws Exception {
        api.owner(post("/api/v1/lactations"), Map.of("animalId", cow, "dateDebut", debut.toString()))
                .andExpect(status().isCreated());
    }

    private UUID activeRation(UUID cow) throws Exception {
        UUID aliment = api.id(api.owner(post("/api/v1/aliments"), Map.of(
                "code", "FOIN-" + UUID.randomUUID().toString().substring(0, 6), "nom", "Foin", "categorie", "FOURRAGE", "unite", "KG"
        )).andExpect(status().isCreated()));
        UUID ration = api.id(api.owner(post("/api/v1/animals/" + cow + "/rations"), Map.of(
                "dateDebut", LocalDate.now().minusDays(10).toString(), "origine", "ACTUELLE"
        )).andExpect(status().isCreated()));
        api.owner(post("/api/v1/animals/" + cow + "/rations/" + ration + "/lines"), Map.of("alimentId", aliment, "quantite", 5))
                .andExpect(status().isOk());
        api.owner(post("/api/v1/animals/" + cow + "/rations/" + ration + "/activate"), null).andExpect(status().isOk());
        return ration;
    }

    private UUID inseminate(UUID cow, LocalDate date) throws Exception {
        return api.id(api.owner(post("/api/v1/reproduction/inseminations"), Map.of(
                "animalId", cow, "dateInsemination", date.toString(), "methodeReproduction", "SAILLIE_NATURELLE"
        )).andExpect(status().isCreated()));
    }

    private org.springframework.test.web.servlet.ResultActions constat(UUID cycle, String resultat) throws Exception {
        return api.owner(post("/api/v1/reproduction/cycles/" + cycle + "/constat"), Map.of(
                "dateConstat", LocalDate.now().minusDays(1).toString(), "resultat", resultat
        ));
    }
}
