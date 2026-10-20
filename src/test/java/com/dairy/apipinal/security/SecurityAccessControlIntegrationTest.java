package com.dairy.apipinal.security;

import com.dairy.apipinal.TestcontainersConfiguration;
import com.dairy.apipinal.animal.infrastructure.persistence.RaceRepository;
import com.dairy.apipinal.identity.infrastructure.security.JwtTokenProvider;
import com.dairy.apipinal.shared.security.AuthenticationMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Preuve de sécurité et d'isolation, par HTTP, sur une base PostgreSQL réelle (Testcontainers).
 * <p>
 * Trois exploitations : A et C partagent le même tenant (isolation intra-tenant), B appartient à un autre
 * tenant. Le propriétaire de A ne doit ni lire ni modifier, par identifiant, les données de B ou de C.
 */
@SpringBootTest(properties = "pinal.security.auth-rate-limit.requests-per-minute=0")
@Import(TestcontainersConfiguration.class)
class SecurityAccessControlIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private RaceRepository raceRepository;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private MockMvc mvc;

    private UUID tenantAC;
    private UUID exploitationA;
    private UUID exploitationC;
    private UUID exploitationB;
    private UUID ownerA;
    private UUID ownerB;
    private UUID ownerC;
    private UUID workerA;
    private UUID gerantA;
    private String phoneWorkerA;
    private UUID raceId;

    // Données de B (autre tenant) et de C (même tenant que A)
    private UUID animalB;
    private UUID animalC;
    private UUID cycleB;
    private UUID lactationB;
    private UUID healthEventB;
    private UUID alimentB;
    private UUID rationB;
    // Données de A
    private UUID animalA;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        raceId = raceRepository.findAll().getFirst().getId();
        tenantAC = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        exploitationA = UUID.randomUUID();
        exploitationB = UUID.randomUUID();
        exploitationC = UUID.randomUUID();
        insertTenant(tenantAC);
        insertTenant(tenantB);
        insertExploitation(exploitationA, tenantAC, "Ferme A");
        insertExploitation(exploitationC, tenantAC, "Ferme C");
        insertExploitation(exploitationB, tenantB, "Ferme B");

        ownerA = insertUser(null);
        ownerB = insertUser(null);
        ownerC = insertUser(null);
        phoneWorkerA = uniquePhone();
        workerA = insertUser(phoneWorkerA);
        gerantA = insertUser(null);
        insertMembership(ownerA, exploitationA, "PROPRIETAIRE");
        insertMembership(ownerB, exploitationB, "PROPRIETAIRE");
        insertMembership(ownerC, exploitationC, "PROPRIETAIRE");
        insertMembership(workerA, exploitationA, "EMPLOYE");
        insertMembership(gerantA, exploitationA, "GERANT");

        animalA = createFemale(ownerA, exploitationA, "A-001");
        animalC = createFemale(ownerC, exploitationC, "C-001");
        animalB = createFemale(ownerB, exploitationB, "B-001");

        cycleB = id(perform(post("/api/v1/reproduction/inseminations"), ownerB, exploitationB, Map.of(
                "animalId", animalB,
                "dateInsemination", LocalDate.now().minusDays(120).toString(),
                "methodeReproduction", "SAILLIE_NATURELLE"
        )).andExpect(status().isCreated()));
        perform(post("/api/v1/reproduction/cycles/" + cycleB + "/constat"), ownerB, exploitationB, Map.of(
                "dateConstat", LocalDate.now().minusDays(60).toString(),
                "resultat", "POSITIF"
        )).andExpect(status().isOk());

        lactationB = id(perform(post("/api/v1/lactations"), ownerB, exploitationB, Map.of(
                "animalId", animalB,
                "dateDebut", LocalDate.now().minusDays(10).toString()
        )).andExpect(status().isCreated()));

        healthEventB = id(perform(post("/api/v1/animals/" + animalB + "/health-events"), ownerB, exploitationB, Map.of(
                "dateHeure", OffsetDateTime.now().minusDays(1).toString(),
                "description", "Boiterie"
        )).andExpect(status().isCreated()));

        alimentB = id(perform(post("/api/v1/aliments"), ownerB, exploitationB, Map.of(
                "code", "FOIN-B", "nom", "Foin B", "categorie", "FOURRAGE", "unite", "KG"
        )).andExpect(status().isCreated()));

        rationB = id(perform(post("/api/v1/animals/" + animalB + "/rations"), ownerB, exploitationB, Map.of(
                "dateDebut", LocalDate.now().minusDays(5).toString(), "origine", "ACTUELLE"
        )).andExpect(status().isCreated()));
    }

    // ------------------------------------------------------------------
    // Isolation entre exploitations
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Le propriétaire de A ne peut pas lire les données d'une autre exploitation par identifiant")
    class CrossExploitationReads {

        @Test
        void animalsOfOtherTenantAndOfSameTenantAreInvisible() throws Exception {
            perform(get("/api/v1/animals/" + animalB), ownerA, exploitationA, null).andExpect(status().isBadRequest());
            perform(get("/api/v1/animals/" + animalC), ownerA, exploitationA, null).andExpect(status().isBadRequest());
            perform(get("/api/v1/worker-hub/animals/" + animalB), ownerA, exploitationA, null).andExpect(status().isBadRequest());
        }

        @Test
        void businessHistoryOfForeignAnimalIsEmptyOrRefused() throws Exception {
            perform(get("/api/v1/reproduction/animaux/" + animalB + "/cycles"), ownerA, exploitationA, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
            perform(get("/api/v1/animals/" + animalB + "/health-events"), ownerA, exploitationA, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
            perform(get("/api/v1/animals/" + animalB + "/health-events/" + healthEventB), ownerA, exploitationA, null)
                    .andExpect(status().isBadRequest());
            perform(get("/api/v1/lactations/" + lactationB), ownerA, exploitationA, null).andExpect(status().isBadRequest());
            perform(get("/api/v1/lactations/animals/" + animalB + "/active"), ownerA, exploitationA, null)
                    .andExpect(status().isNoContent());
            perform(get("/api/v1/lactations/" + lactationB + "/analyses"), ownerA, exploitationA, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
            perform(get("/api/v1/lactations/" + lactationB + "/milkings/courbe"), ownerA, exploitationA, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
            perform(get("/api/v1/animals/" + animalB + "/rations"), ownerA, exploitationA, null).andExpect(status().isBadRequest());
            perform(get("/api/v1/animals/" + animalB + "/rations/" + rationB), ownerA, exploitationA, null)
                    .andExpect(status().isBadRequest());
        }

        @Test
        void feedCatalogueAndPricesAreOwnedByEachExploitation() throws Exception {
            String catalogueA = perform(get("/api/v1/aliments"), ownerA, exploitationA, null)
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(catalogueA).doesNotContain(alimentB.toString());
            perform(get("/api/v1/aliments/" + alimentB + "/prices"), ownerA, exploitationA, null)
                    .andExpect(status().isBadRequest());
        }

        @Test
        void clientSuppliedExploitationMustMatchContext() throws Exception {
            perform(get("/api/v1/animals?exploitationId=" + exploitationB), ownerA, exploitationA, null)
                    .andExpect(status().isForbidden());
            perform(get("/api/v1/animals?exploitationId=" + exploitationC), ownerA, exploitationA, null)
                    .andExpect(status().isForbidden());
            perform(get("/api/v1/worker-hub/home?exploitationId=" + exploitationC), ownerA, exploitationA, null)
                    .andExpect(status().isForbidden());
        }

        @Test
        void spoofedExploitationHeaderIsRejected() throws Exception {
            perform(get("/api/v1/animals"), ownerA, exploitationB, null).andExpect(status().isForbidden());
            perform(get("/api/v1/animals"), ownerA, exploitationC, null).andExpect(status().isForbidden());
            perform(get("/api/v1/finance/dashboard"), ownerA, exploitationB, null).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Le propriétaire de A ne peut pas modifier les données d'une autre exploitation par identifiant")
    class CrossExploitationWrites {

        @Test
        void animalLifecycleOperationsAreRefused() throws Exception {
            perform(post("/api/v1/animals/" + animalB + "/sortie"), ownerA, exploitationA, Map.of(
                    "dateSortie", LocalDate.now().toString(), "motif", "VENTE", "prixVente", 100
            )).andExpect(status().isBadRequest());
            perform(patch("/api/v1/animals/" + animalC), ownerA, exploitationA, Map.of(
                    "raceId", raceId, "identifiant", "PIRATE", "nom", "Pirate", "sexe", "FEMELLE"
            )).andExpect(status().isBadRequest());
            perform(delete("/api/v1/animals/" + animalB), ownerA, exploitationA, null).andExpect(status().isBadRequest());

            assertThat(jdbc.queryForObject("SELECT statut FROM animal WHERE id = ?", String.class, animalB)).isEqualTo("ACTIF");
            assertThat(jdbc.queryForObject("SELECT identifiant FROM animal WHERE id = ?", String.class, animalC)).isEqualTo("C-001");
        }

        @Test
        void reproductionCommandsOnForeignCycleAreRefused() throws Exception {
            perform(post("/api/v1/reproduction/cycles/" + cycleB + "/velage"), ownerA, exploitationA, Map.of(
                    "dateReelle", LocalDate.now().toString(), "veaux", List.of()
            )).andExpect(status().isBadRequest());
            perform(post("/api/v1/reproduction/cycles/" + cycleB + "/avortement"), ownerA, exploitationA, null)
                    .andExpect(status().isBadRequest());
            perform(post("/api/v1/reproduction/cycles/" + cycleB + "/constat"), ownerA, exploitationA, Map.of(
                    "dateConstat", LocalDate.now().toString(), "resultat", "NEGATIF"
            )).andExpect(status().isBadRequest());
            perform(post("/api/v1/reproduction/inseminations"), ownerA, exploitationA, Map.of(
                    "animalId", animalC, "dateInsemination", LocalDate.now().toString(),
                    "methodeReproduction", "SAILLIE_NATURELLE"
            )).andExpect(status().isBadRequest());

            assertThat(jdbc.queryForObject("SELECT statut FROM cycle_reproduction WHERE id = ?", String.class, cycleB))
                    .isEqualTo("GESTANTE");
        }

        @Test
        void productionHealthAndNutritionWritesOnForeignDataAreRefused() throws Exception {
            perform(post("/api/v1/animals/" + animalB + "/milkings"), ownerA, exploitationA, milking(10))
                    .andExpect(status().isBadRequest());
            perform(post("/api/v1/lactations/" + lactationB + "/milkings"), ownerA, exploitationA, milking(10))
                    .andExpect(status().isBadRequest());
            perform(post("/api/v1/lactations/animals/" + animalB + "/tarir"), ownerA, exploitationA, null)
                    .andExpect(status().isConflict());
            perform(post("/api/v1/animals/" + animalB + "/health-events"), ownerA, exploitationA, Map.of(
                    "dateHeure", OffsetDateTime.now().toString(), "description", "Intrusion"
            )).andExpect(status().isBadRequest());
            perform(patch("/api/v1/animals/" + animalB + "/health-events/" + healthEventB), ownerA, exploitationA, Map.of(
                    "description", "Modifié"
            )).andExpect(status().isBadRequest());
            perform(post("/api/v1/aliments/" + alimentB + "/prices"), ownerA, exploitationA, Map.of(
                    "prixUnitaire", 1, "dateDebut", LocalDate.now().toString()
            )).andExpect(status().isBadRequest());
            perform(post("/api/v1/animals/" + animalB + "/rations"), ownerA, exploitationA, Map.of(
                    "dateDebut", LocalDate.now().toString(), "origine", "ACTUELLE"
            )).andExpect(status().isBadRequest());
            perform(post("/api/v1/animals/" + animalB + "/rations/" + rationB + "/activate"), ownerA, exploitationA, null)
                    .andExpect(status().isBadRequest());
            perform(post("/api/v1/animals"), ownerA, exploitationA, Map.of(
                    "exploitationId", exploitationB, "raceId", raceId, "identifiant", "X-1", "nom", "X", "sexe", "FEMELLE"
            )).andExpect(status().isForbidden());

            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM traite WHERE lactation_id = ?", Integer.class, lactationB)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM prix_aliment WHERE aliment_id = ?", Integer.class, alimentB)).isZero();
            assertThat(jdbc.queryForObject("SELECT statut FROM ration WHERE id = ?", String.class, rationB)).isEqualTo("BROUILLON");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM animal WHERE exploitation_id = ?", Integer.class, exploitationB)).isEqualTo(1);
        }
    }

    // ------------------------------------------------------------------
    // Rôles
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Un ouvrier n'accède qu'aux fonctions du parcours ouvrier")
    class WorkerRole {

        @Test
        void workerCanUseWorkerJourney() throws Exception {
            performPin(get("/api/v1/worker-hub/home"), workerA, exploitationA, null).andExpect(status().isOk());
            performPin(get("/api/v1/worker-hub/animals"), workerA, exploitationA, null).andExpect(status().isOk());
            performPin(post("/api/v1/animals/" + animalA + "/health-events"), workerA, exploitationA, Map.of(
                    "dateHeure", OffsetDateTime.now().toString(), "description", "Toux"
            )).andExpect(status().isCreated());
        }

        @Test
        void workerCannotUseOwnerFunctions() throws Exception {
            List<MockHttpServletRequestBuilder> ownerOnly = List.of(
                    get("/api/v1/dashboard/summary"),
                    get("/api/v1/finance/dashboard"),
                    get("/api/v1/production/dashboard"),
                    get("/api/v1/health/dashboard"),
                    get("/api/v1/nutrition/dashboard"),
                    get("/api/v1/animals"),
                    get("/api/v1/animals/" + animalA),
                    delete("/api/v1/animals/" + animalA),
                    get("/api/v1/aliments"),
                    get("/api/v1/animals/" + animalA + "/health-events"),
                    get("/api/v1/reproduction/animaux/" + animalA + "/cycles"),
                    get("/api/v1/exploitations/" + exploitationA + "/members")
            );
            for (MockHttpServletRequestBuilder request : ownerOnly) {
                performPin(request, workerA, exploitationA, null).andExpect(status().isForbidden());
            }
            performPin(post("/api/v1/aliments"), workerA, exploitationA, Map.of(
                    "code", "X", "nom", "X", "categorie", "X", "unite", "KG"
            )).andExpect(status().isForbidden());
            performPin(post("/api/v1/finance/charges"), workerA, exploitationA, Map.of(
                    "libelle", "X", "categorie", "AUTRE", "montant", 1, "date", LocalDate.now().toString()
            )).andExpect(status().isForbidden());
            performPin(post("/api/v1/exploitations"), workerA, null, Map.of("nom", "Nouvelle ferme"))
                    .andExpect(status().isForbidden());

            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM animal WHERE id = ?", Integer.class, animalA)).isEqualTo(1);
        }

        @Test
        void pinSessionNeverOpensAnOwnerMembership() throws Exception {
            // Le propriétaire de A authentifié par PIN (PIN connu de celui qui l'a remis) n'accède pas à A.
            performPin(get("/api/v1/dashboard/summary"), ownerA, exploitationA, null).andExpect(status().isForbidden());
            performPin(get("/api/v1/worker-hub/home"), ownerA, exploitationA, null).andExpect(status().isForbidden());
            performPin(get("/api/v1/exploitations"), ownerA, null, null)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        }
    }

    // ------------------------------------------------------------------
    // Équipe et PIN
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Gestion d'équipe")
    class TeamManagement {

        @Test
        void onlyOwnerManagesTeamAndPinsAreNeverListed() throws Exception {
            String members = perform(get("/api/v1/exploitations/" + exploitationA + "/members"), ownerA, exploitationA, null)
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(members).doesNotContainIgnoringCase("pin");

            perform(post("/api/v1/exploitations/" + exploitationA + "/members"), gerantA, exploitationA, Map.of(
                    "telephone", uniquePhone(), "nomComplet", "Faux Proprio", "role", "EMPLOYE"
            )).andExpect(status().isForbidden());

            perform(post("/api/v1/exploitations/" + exploitationA + "/members"), ownerA, exploitationA, Map.of(
                    "telephone", uniquePhone(), "nomComplet", "Nouveau Proprio", "role", "PROPRIETAIRE"
            )).andExpect(status().isBadRequest());

            perform(post("/api/v1/exploitations/" + exploitationB + "/members"), ownerA, exploitationA, Map.of(
                    "telephone", uniquePhone(), "nomComplet", "Intrus", "role", "EMPLOYE"
            )).andExpect(status().isForbidden());
        }

        @Test
        void invitedWorkerReceivesPinOnceAndItIsStoredHashed() throws Exception {
            String phone = uniquePhone();
            JsonNode invitation = body(perform(post("/api/v1/exploitations/" + exploitationA + "/members"), ownerA, exploitationA, Map.of(
                    "telephone", phone, "nomComplet", "Moussa Ba", "role", "EMPLOYE"
            )).andExpect(status().isCreated()));

            String pin = invitation.get("codePin").asText();
            assertThat(pin).matches("\\d{6}");
            String stored = jdbc.queryForObject("SELECT pin_hash FROM utilisateur WHERE telephone = ?", String.class, phone);
            assertThat(stored).startsWith("$2").isNotEqualTo(pin);

            mvc.perform(post("/api/v1/auth/pin/verify").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("telephone", phone, "code", pin))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }

        @Test
        void ownersCreatedByOtpHaveNoPinAndProfileNeverExposesIt() throws Exception {
            String profile = perform(get("/api/v1/identity/users/me"), ownerA, null, null)
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(profile).doesNotContainIgnoringCase("pin");
        }
    }

    @Nested
    @DisplayName("Protection contre les tentatives répétées")
    class BruteForce {

        @Test
        void pinIsLockedAfterRepeatedFailuresAndUnknownUserGetsSameAnswer() throws Exception {
            String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("482913");
            jdbc.update("UPDATE utilisateur SET pin_hash = ? WHERE id = ?", hash, workerA);

            String unknown = pinVerify(uniquePhone(), "000000").andExpect(status().isUnauthorized())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            String wrong = pinVerify(phoneWorkerA, "000000").andExpect(status().isUnauthorized())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(json.readTree(unknown).get("detail")).isEqualTo(json.readTree(wrong).get("detail"));

            for (int i = 0; i < 3; i++) {
                pinVerify(phoneWorkerA, "000000").andExpect(status().isUnauthorized());
            }
            pinVerify(phoneWorkerA, "000000").andExpect(status().isTooManyRequests());
            // Même le bon PIN est refusé pendant le verrouillage.
            pinVerify(phoneWorkerA, "482913").andExpect(status().isTooManyRequests());
            assertThat(jdbc.queryForObject("SELECT pin_locked_until FROM utilisateur WHERE id = ?", OffsetDateTime.class, workerA))
                    .isAfter(OffsetDateTime.now());
        }

        @Test
        void otpAttemptsArePersistedAndCodeIsBurnedAfterMaxAttempts() throws Exception {
            String phone = uniquePhone();
            jdbc.update("INSERT INTO otp_code (id, telephone, code_hash, expires_at, attempts, used, created_at) "
                            + "VALUES (?, ?, ?, ?, 0, false, NOW())",
                    UUID.randomUUID(), phone, sha256("123456"), OffsetDateTime.now().plusMinutes(5));

            for (int i = 0; i < 5; i++) {
                otpVerify(phone, "999999").andExpect(status().isUnauthorized());
            }
            assertThat(jdbc.queryForObject("SELECT attempts FROM otp_code WHERE telephone = ?", Integer.class, phone)).isEqualTo(5);
            otpVerify(phone, "123456").andExpect(status().isTooManyRequests());
        }
    }

    @Test
    @DisplayName("Le jeu de démonstration n'est pas chargé hors du profil demo")
    void demoDatasetIsNotPartOfStandardInitialisation() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tenant WHERE id = 'b0000000-0000-0000-0000-000000000001'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM utilisateur WHERE pin_hash = '1234'", Integer.class)).isZero();
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    private ResultActions perform(MockHttpServletRequestBuilder request, UUID user, UUID exploitation, Object body) throws Exception {
        return send(request, jwtTokenProvider.generateAccessToken(user, AuthenticationMethod.OTP), exploitation, body);
    }

    private ResultActions performPin(MockHttpServletRequestBuilder request, UUID user, UUID exploitation, Object body) throws Exception {
        return send(request, jwtTokenProvider.generateAccessToken(user, AuthenticationMethod.PIN), exploitation, body);
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String token, UUID exploitation, Object body) throws Exception {
        request.header("Authorization", "Bearer " + token);
        if (exploitation != null) {
            request.header("X-Exploitation-ID", exploitation.toString());
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(request);
    }

    private ResultActions pinVerify(String phone, String pin) throws Exception {
        return mvc.perform(post("/api/v1/auth/pin/verify").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("telephone", phone, "code", pin))));
    }

    private ResultActions otpVerify(String phone, String code) throws Exception {
        return mvc.perform(post("/api/v1/auth/otp/verify").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("telephone", phone, "code", code))));
    }

    private Map<String, Object> milking(double quantity) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateHeure", OffsetDateTime.now().toString());
        body.put("type", "MATIN");
        body.put("quantiteLitres", quantity);
        return body;
    }

    private UUID createFemale(UUID owner, UUID exploitation, String identifiant) throws Exception {
        return id(perform(post("/api/v1/animals"), owner, exploitation, Map.of(
                "raceId", raceId, "identifiant", identifiant, "nom", "Vache " + identifiant,
                "sexe", "FEMELLE", "dateNaissance", LocalDate.now().minusYears(4).toString()
        )).andExpect(status().isCreated()));
    }

    private UUID id(ResultActions actions) throws Exception {
        return UUID.fromString(body(actions).get("id").asText());
    }

    private JsonNode body(ResultActions actions) throws Exception {
        MvcResult result = actions.andReturn();
        return json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void insertTenant(UUID id) {
        jdbc.update("INSERT INTO tenant (id, nom, statut, created_at, updated_at, version) VALUES (?, 'Tenant', 'ACTIF', NOW(), NOW(), 0)", id);
    }

    private void insertExploitation(UUID id, UUID tenant, String nom) {
        jdbc.update("INSERT INTO exploitation (id, tenant_id, nom, localite, actif, created_at, updated_at, version) "
                + "VALUES (?, ?, ?, 'Thiès', true, NOW(), NOW(), 0)", id, tenant, nom);
    }

    private UUID insertUser(String phone) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO utilisateur (id, nom, prenom, statut, telephone, created_at, updated_at, version) "
                + "VALUES (?, 'Test', 'Utilisateur', 'ACTIF', ?, NOW(), NOW(), 0)", id, phone != null ? phone : uniquePhone());
        return id;
    }

    private void insertMembership(UUID user, UUID exploitation, String role) {
        jdbc.update("INSERT INTO membership (id, utilisateur_id, exploitation_id, role, created_at, updated_at, version) "
                + "VALUES (?, ?, ?, ?, NOW(), NOW(), 0)", UUID.randomUUID(), user, exploitation, role);
    }

    private static String uniquePhone() {
        return "+2217" + String.format("%08d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000L));
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
