package com.dairy.apipinal;

import com.dairy.apipinal.animal.domain.SexeAnimal;
import com.dairy.apipinal.animal.infrastructure.persistence.RaceRepository;
import com.dairy.apipinal.animal.infrastructure.web.CreateAnimalRequest;
import com.dairy.apipinal.animal.infrastructure.web.MouvementSortieRequest;
import com.dairy.apipinal.finance.infrastructure.web.PrixVenteRequest;
import com.dairy.apipinal.health.infrastructure.web.RecordHealthEventRequest;
import com.dairy.apipinal.identity.infrastructure.security.JwtTokenProvider;
import com.dairy.apipinal.nutrition.domain.OrigineRation;
import com.dairy.apipinal.nutrition.domain.UniteAliment;
import com.dairy.apipinal.nutrition.infrastructure.web.AddRationLineRequest;
import com.dairy.apipinal.nutrition.infrastructure.web.CreateAlimentRequest;
import com.dairy.apipinal.nutrition.infrastructure.web.CreatePrixAlimentRequest;
import com.dairy.apipinal.nutrition.infrastructure.web.CreateRationRequest;
import com.dairy.apipinal.production.domain.TypeTraite;
import com.dairy.apipinal.production.infrastructure.web.RecordMilkingRequest;
import com.dairy.apipinal.reproduction.infrastructure.web.ConstatGestationRequest;
import com.dairy.apipinal.reproduction.infrastructure.web.InseminationRequest;
import com.dairy.apipinal.reproduction.infrastructure.web.VelageRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BUSINESS REALITY GATE — Testcontainers PostgreSQL
 * 
 * Vérifie que les actions métier entraînent toutes les mutations attendues
 * dans les autres modules, sans données orphelines, sans calculs incohérents,
 * et sans fallback silencieux.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BusinessRealityGateIT {

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private RaceRepository raceRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private UUID tenantId;
    private UUID exploitationId;
    private UUID userId;
    private UUID raceId;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        tenantId = UUID.randomUUID();
        exploitationId = UUID.randomUUID();
        userId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO tenant (id, nom, statut, created_at, updated_at, version) VALUES (?, 'Tenant BRG', 'ACTIF', NOW(), NOW(), 0)",
                tenantId
        );
        jdbcTemplate.update(
                "INSERT INTO exploitation (id, tenant_id, nom, localite, actif, created_at, updated_at, version) VALUES (?, ?, 'Ferme BRG', 'Dakar', true, NOW(), NOW(), 0)",
                exploitationId, tenantId
        );
        jdbcTemplate.update(
                "INSERT INTO utilisateur (id, nom, prenom, statut, telephone, created_at, updated_at, version) VALUES (?, 'Business', 'Reality', 'ACTIF', ?, NOW(), NOW(), 0)",
                userId, "+22177" + (System.currentTimeMillis() % 10000000L)
        );
        jdbcTemplate.update(
                "INSERT INTO membership (id, utilisateur_id, exploitation_id, role, created_at, updated_at, version) VALUES (?, ?, ?, 'PROPRIETAIRE', NOW(), NOW(), 0)",
                UUID.randomUUID(), userId, exploitationId
        );

        raceId = raceRepository.findAll().stream().findFirst().orElseThrow().getId();
        jwtToken = jwtTokenProvider.generateAccessToken(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========================================================================
    // SCÉNARIO 1 : Vêlage → Effets transversaux (Lactation + Création Veau)
    // ========================================================================
    @Test
    @Order(1)
    @DisplayName("BRG-1: Vêlage déclenche lactation + création veau + cohérence cross-module")
    void velage_triggers_lactation_and_calf_creation() throws Exception {
        // 1. Créer la vache mère
        UUID mereId = createAnimal("MERE-BRG-" + System.currentTimeMillis(), "Marguerite", SexeAnimal.FEMELLE);

        // 2. Insémination
        InseminationRequest insemReq = new InseminationRequest(
                mereId, exploitationId, LocalDate.now().minusDays(290),
                com.dairy.apipinal.reproduction.domain.MethodeReproduction.INSEMINATION_ARTIFICIELLE.name(),
                null, "PAILL-BRG-001"
        );
        MvcResult insemResult = mockMvc.perform(post("/api/v1/reproduction/inseminations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insemReq)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode cycleNode = objectMapper.readTree(insemResult.getResponse().getContentAsString());
        UUID cycleId = UUID.fromString(cycleNode.get("id").asText());

        // Vérifier statut EN_ATTENTE_CONSTAT en DB
        String cycleStatut = jdbcTemplate.queryForObject(
                "SELECT statut FROM cycle_reproduction WHERE id = ?", String.class, cycleId);
        assertEquals("EN_ATTENTE_CONSTAT", cycleStatut);

        // 3. Constat positif → GESTANTE
        ConstatGestationRequest constatReq = new ConstatGestationRequest(
                LocalDate.now().minusDays(250), "POSITIF", "Dr. Diallo"
        );
        mockMvc.perform(post("/api/v1/reproduction/cycles/" + cycleId + "/constat")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(constatReq)))
                .andExpect(status().isOk());

        cycleStatut = jdbcTemplate.queryForObject(
                "SELECT statut FROM cycle_reproduction WHERE id = ?", String.class, cycleId);
        assertEquals("GESTANTE", cycleStatut);

        // 4. Vêlage → doit déclencher : cycle TERMINEE_VELAGE + lactation EN_COURS + veau créé
        VelageRequest velageReq = new VelageRequest(
                LocalDate.now().minusDays(5),
                List.of(new VelageRequest.VeauPayload("VEAU-BRG-001", "Petit", "MALE", 1))
        );
        mockMvc.perform(post("/api/v1/reproduction/cycles/" + cycleId + "/velage")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(velageReq)))
                .andExpect(status().isOk());

        // Vérifier cycle → TERMINEE_VELAGE
        cycleStatut = jdbcTemplate.queryForObject(
                "SELECT statut FROM cycle_reproduction WHERE id = ?", String.class, cycleId);
        assertEquals("TERMINEE_VELAGE", cycleStatut);

        // Attendre convergence des événements asynchrones (ApplicationModuleListener)
        Thread.sleep(2000);

        // Vérifier lactation créée pour la mère
        Long lactationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lactation WHERE animal_id = ? AND statut = 'EN_COURS' AND tenant_id = ?",
                Long.class, mereId, tenantId);
        assertEquals(1L, lactationCount, "Une lactation EN_COURS doit exister pour la mère après vêlage");

        // Vérifier veau créé dans la même exploitation
        Long veauCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM animal WHERE identifiant = 'VEAU-BRG-001' AND exploitation_id = ? AND tenant_id = ?",
                Long.class, exploitationId, tenantId);
        assertEquals(1L, veauCount, "Le veau doit être créé dans la même exploitation");

        // Vérifier que le veau a le bon sexe et statut
        Map<String, Object> veauRow = jdbcTemplate.queryForMap(
                "SELECT sexe, statut, mere_id FROM animal WHERE identifiant = 'VEAU-BRG-001' AND tenant_id = ?", tenantId);
        assertEquals("MALE", veauRow.get("sexe"));
        assertEquals("ACTIF", veauRow.get("statut"));
        assertEquals(mereId, veauRow.get("mere_id"));
    }

    // ========================================================================
    // SCÉNARIO 2 : Sortie animal → Clôture lactation + rations
    // ========================================================================
    @Test
    @Order(2)
    @DisplayName("BRG-2: Sortie animal → clôture lactation + ration active")
    void animal_exit_closes_lactation_and_rations() throws Exception {
        // 1. Créer vache
        UUID cowId = createAnimal("EXIT-BRG-" + System.currentTimeMillis(), "Sortante", SexeAnimal.FEMELLE);

        // 2. Démarrer une lactation manuellement
        com.dairy.apipinal.production.infrastructure.web.StartLactationRequest startLactReq =
                new com.dairy.apipinal.production.infrastructure.web.StartLactationRequest(
                        cowId, LocalDate.now().minusDays(30)
                );
        MvcResult lactResult = mockMvc.perform(post("/api/v1/lactations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startLactReq)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID lactationId = UUID.fromString(
                objectMapper.readTree(lactResult.getResponse().getContentAsString()).get("id").asText());

        // 3. Créer un aliment et une ration active
        UUID alimentId = createAliment("Foin BRG", "KG");
        UUID rationId = createAndActivateRation(cowId, alimentId);

        // Vérifier pré-conditions
        String lactStatut = jdbcTemplate.queryForObject(
                "SELECT statut FROM lactation WHERE id = ?", String.class, lactationId);
        assertEquals("EN_COURS", lactStatut);
        String rationStatut = jdbcTemplate.queryForObject(
                "SELECT statut FROM ration WHERE id = ?", String.class, rationId);
        assertEquals("ACTIVE", rationStatut);

        // 4. Déclarer sortie (vente)
        MouvementSortieRequest sortieReq = new MouvementSortieRequest(
                LocalDate.now(), "VENTE", new BigDecimal("250000")
        );
        mockMvc.perform(post("/api/v1/animals/" + cowId + "/sortie")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sortieReq)))
                .andExpect(status().isOk());

        // Attendre propagation événements
        Thread.sleep(2000);

        // Vérifier animal → VENDU
        String animalStatut = jdbcTemplate.queryForObject(
                "SELECT statut FROM animal WHERE id = ?", String.class, cowId);
        assertEquals("VENDU", animalStatut);

        // Vérifier lactation → TERMINEE
        lactStatut = jdbcTemplate.queryForObject(
                "SELECT statut FROM lactation WHERE id = ?", String.class, lactationId);
        assertEquals("TERMINEE", lactStatut, "La lactation doit être clôturée après sortie de l'animal");

        // Vérifier ration → TERMINEE
        rationStatut = jdbcTemplate.queryForObject(
                "SELECT statut FROM ration WHERE id = ?", String.class, rationId);
        assertEquals("TERMINEE", rationStatut, "La ration doit être terminée après sortie de l'animal");
    }

    // ========================================================================
    // SCÉNARIO 3 : KPI Finance reconstructible depuis sources de vérité
    // ========================================================================
    @Test
    @Order(3)
    @DisplayName("BRG-3: KPI Finance = sources de vérité (traites × prix × conversion)")
    void finance_kpi_matches_source_of_truth() throws Exception {
        // 1. Créer vache + lactation
        UUID cowId = createAnimal("FIN-BRG-" + System.currentTimeMillis(), "Financière", SexeAnimal.FEMELLE);

        com.dairy.apipinal.production.infrastructure.web.StartLactationRequest startLactReq =
                new com.dairy.apipinal.production.infrastructure.web.StartLactationRequest(
                        cowId, LocalDate.now().minusDays(10)
                );
        MvcResult lactResult = mockMvc.perform(post("/api/v1/lactations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startLactReq)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID lactationId = UUID.fromString(
                objectMapper.readTree(lactResult.getResponse().getContentAsString()).get("id").asText());

        // 2. Enregistrer 2 traites
        BigDecimal traite1Kg = new BigDecimal("8.5");
        BigDecimal traite2Kg = new BigDecimal("7.0");
        recordMilking(lactationId, traite1Kg, TypeTraite.MATIN);
        recordMilking(lactationId, traite2Kg, TypeTraite.SOIR);

        // 3. Configurer prix du lait
        PrixVenteRequest prixReq = new PrixVenteRequest(
                new BigDecimal("500.00"), LocalDate.now().minusDays(30), null
        );
        mockMvc.perform(post("/api/v1/finance/prix-vente")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prixReq)))
                .andExpect(status().isCreated());

        // 4. Vérifier total production en DB
        BigDecimal totalKgDb = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(quantite_kg), 0) FROM traite WHERE lactation_id = ? AND tenant_id = ?",
                BigDecimal.class, lactationId, tenantId);
        assertEquals(0, traite1Kg.add(traite2Kg).compareTo(totalKgDb),
                "La somme des traites en DB doit correspondre exactement");

        // 5. Vérifier prix applicable en DB
        BigDecimal prixDb = jdbcTemplate.queryForObject(
                "SELECT prix_par_litre FROM prix_vente_lait WHERE tenant_id = ? AND date_debut <= CURRENT_DATE AND (date_fin IS NULL OR date_fin >= CURRENT_DATE) ORDER BY date_debut DESC LIMIT 1",
                BigDecimal.class, tenantId);
        assertEquals(0, new BigDecimal("500.00").compareTo(prixDb));

        // 6. Le CA attendu = totalKg × 0.971 (conversion) × prix
        BigDecimal expectedLitres = totalKgDb.multiply(new BigDecimal("0.971"));
        BigDecimal expectedCA = expectedLitres.multiply(prixDb);

        assertTrue(expectedCA.signum() > 0, "Le CA doit être strictement positif");

        MvcResult dashboardResult = mockMvc.perform(get("/api/v1/finance/dashboard")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode finance = objectMapper.readTree(dashboardResult.getResponse().getContentAsString());
        assertEquals(0, expectedCA.compareTo(finance.get("kpis").get("chiffreAffaires").decimalValue()),
                "Le CA retourné par l'API doit correspondre aux traites converties et au prix historique");
    }

    // ========================================================================
    // SCÉNARIO 4 : Avortement → événement sanitaire + cycle AVORTEE
    // ========================================================================
    @Test
    @Order(4)
    @DisplayName("BRG-4: Avortement → cycle AVORTEE + événement sanitaire créé")
    void avortement_triggers_health_event() throws Exception {
        UUID cowId = createAnimal("AVORT-BRG-" + System.currentTimeMillis(), "Avortée", SexeAnimal.FEMELLE);

        // Insémination
        InseminationRequest insemReq = new InseminationRequest(
                cowId, exploitationId, LocalDate.now().minusDays(100),
                com.dairy.apipinal.reproduction.domain.MethodeReproduction.SAILLIE_NATURELLE.name(),
                null, null
        );
        MvcResult insemResult = mockMvc.perform(post("/api/v1/reproduction/inseminations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insemReq)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID cycleId = UUID.fromString(
                objectMapper.readTree(insemResult.getResponse().getContentAsString()).get("id").asText());

        // Constat positif → GESTANTE
        mockMvc.perform(post("/api/v1/reproduction/cycles/" + cycleId + "/constat")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ConstatGestationRequest(LocalDate.now().minusDays(60), "POSITIF", "Dr. Fall"))))
                .andExpect(status().isOk());

        // Déclarer avortement
        mockMvc.perform(post("/api/v1/reproduction/cycles/" + cycleId + "/avortement")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andExpect(status().isOk());

        // Attendre propagation
        Thread.sleep(2000);

        // Vérifier cycle → AVORTEE
        String statut = jdbcTemplate.queryForObject(
                "SELECT statut FROM cycle_reproduction WHERE id = ?", String.class, cycleId);
        assertEquals("AVORTEE", statut);

        // Vérifier événement sanitaire créé pour cet avortement
        Long healthEventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM evenement_sanitaire WHERE animal_id = ? AND source_cycle_id = ? AND tenant_id = ?",
                Long.class, cowId, cycleId, tenantId);
        assertTrue(healthEventCount > 0, "Un événement sanitaire doit être créé suite à l'avortement");
    }

    // ========================================================================
    // SCÉNARIO 5 : Données manquantes restent nulles (pas de faux zéro)
    // ========================================================================
    @Test
    @Order(5)
    @DisplayName("BRG-5: Données manquantes = null, pas de faux zéro ou N/A")
    void missing_data_stays_null() throws Exception {
        // Dashboard sans aucune donnée de production
        MvcResult dashResult = mockMvc.perform(get("/api/v1/dashboard/summary")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode dashboard = objectMapper.readTree(dashResult.getResponse().getContentAsString());

        // Production doit être 0 (pas null) car c'est un agrégat
        assertEquals(0, dashboard.get("production").get("kgAujourdhui").asDouble());
        assertEquals(0, dashboard.get("production").get("kgDerniers7Jours").asDouble());

        // Finance : sans prix de vente configuré, CA estimé doit être null
        assertTrue(dashboard.get("finance").get("chiffreAffairesEstimeLait30Jours").isNull(),
                "Le CA estimé doit être null quand aucun prix de vente n'est configuré, pas 0");
        assertTrue(dashboard.get("finance").get("margeEstimeeSurCoutAlimentaire30Jours").isNull(),
                "La marge estimée doit être null quand aucun prix n'est configuré");
    }

    // ========================================================================
    // SCÉNARIO 6 : Unité unique kg dans toute la chaîne traite
    // ========================================================================
    @Test
    @Order(6)
    @DisplayName("BRG-6: Unité kg cohérente de la saisie UI à la DB")
    void unit_consistency_kg_throughout() throws Exception {
        UUID cowId = createAnimal("UNIT-BRG-" + System.currentTimeMillis(), "Unitaire", SexeAnimal.FEMELLE);

        com.dairy.apipinal.production.infrastructure.web.StartLactationRequest startLactReq =
                new com.dairy.apipinal.production.infrastructure.web.StartLactationRequest(
                        cowId, LocalDate.now().minusDays(5)
                );
        MvcResult lactResult = mockMvc.perform(post("/api/v1/lactations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startLactReq)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID lactationId = UUID.fromString(
                objectMapper.readTree(lactResult.getResponse().getContentAsString()).get("id").asText());

        BigDecimal inputKg = new BigDecimal("12.345");
        recordMilking(lactationId, inputKg, TypeTraite.MATIN);

        // Vérifier en DB : colonne est quantite_kg, pas quantite_litres
        BigDecimal dbKg = jdbcTemplate.queryForObject(
                "SELECT quantite_kg FROM traite WHERE lactation_id = ? AND tenant_id = ? ORDER BY date_heure DESC LIMIT 1",
                BigDecimal.class, lactationId, tenantId);
        assertEquals(0, inputKg.compareTo(dbKg),
                "La valeur en DB (quantite_kg) doit être identique à la saisie UI");

        // Vérifier via API GET
        MvcResult getResult = mockMvc.perform(get("/api/v1/lactations/" + lactationId + "/milkings/courbe")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode traites = objectMapper.readTree(getResult.getResponse().getContentAsString());
        assertTrue(traites.isArray() && traites.size() > 0);
        assertEquals(0, inputKg.compareTo(new BigDecimal(traites.get(0).get("quantiteKg").asText())),
                "La valeur retournée par l'API doit être en kg, identique à la DB");
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private UUID createAnimal(String identifiant, String nom, SexeAnimal sexe) throws Exception {
        CreateAnimalRequest req = new CreateAnimalRequest(
                exploitationId, raceId, identifiant, nom, null,
                LocalDate.of(2021, 1, 1), null, null, sexe
        );
        MvcResult result = mockMvc.perform(post("/api/v1/animals")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private void recordMilking(UUID lactationId, BigDecimal quantiteKg, TypeTraite type) throws Exception {
        RecordMilkingRequest req = new RecordMilkingRequest(
                OffsetDateTime.now(), type, quantiteKg
        );
        mockMvc.perform(post("/api/v1/lactations/" + lactationId + "/milkings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    private UUID createAliment(String nom, String unite) throws Exception {
        CreateAlimentRequest req = new CreateAlimentRequest(
                UUID.randomUUID().toString(), nom, "FOURRAGE", UniteAliment.valueOf(unite));
        MvcResult result = mockMvc.perform(post("/api/v1/aliments")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createAndActivateRation(UUID animalId, UUID alimentId) throws Exception {
        // 1. Create ration (BROUILLON)
        CreateRationRequest createReq = new CreateRationRequest(
                LocalDate.now().minusDays(10), OrigineRation.ACTUELLE
        );
        MvcResult createResult = mockMvc.perform(post("/api/v1/animals/" + animalId + "/rations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID rationId = UUID.fromString(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        // 2. Set prix aliment
        CreatePrixAlimentRequest prixReq = new CreatePrixAlimentRequest(
                new BigDecimal("150"), LocalDate.now().minusDays(30), null
        );
        mockMvc.perform(post("/api/v1/aliments/" + alimentId + "/prices")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prixReq)))
                .andExpect(status().isOk());

        // 3. Add ligne
        AddRationLineRequest lineReq = new AddRationLineRequest(alimentId, new BigDecimal("5.0"));
        mockMvc.perform(post("/api/v1/animals/" + animalId + "/rations/" + rationId + "/lines")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lineReq)))
                .andExpect(status().isOk());

        // 4. Activate
        mockMvc.perform(post("/api/v1/animals/" + animalId + "/rations/" + rationId + "/activate")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andExpect(status().isOk());

        return rationId;
    }
}
