package com.dairy.apipinal;

import com.dairy.apipinal.animal.domain.SexeAnimal;
import com.dairy.apipinal.animal.infrastructure.persistence.RaceRepository;
import com.dairy.apipinal.animal.infrastructure.web.CreateAnimalRequest;
import com.dairy.apipinal.animal.infrastructure.web.MouvementSortieRequest;
import com.dairy.apipinal.finance.infrastructure.web.ChargeRequest;
import com.dairy.apipinal.finance.infrastructure.web.PrixVenteRequest;
import com.dairy.apipinal.health.infrastructure.web.RecordHealthEventRequest;
import com.dairy.apipinal.health.infrastructure.web.UpdateHealthEventRequest;
import com.dairy.apipinal.identity.infrastructure.security.JwtTokenProvider;
import com.dairy.apipinal.identity.infrastructure.web.dto.CreateExploitationRequest;
import com.dairy.apipinal.nutrition.domain.OrigineRation;
import com.dairy.apipinal.nutrition.domain.UniteAliment;
import com.dairy.apipinal.nutrition.infrastructure.web.AddRationLineRequest;
import com.dairy.apipinal.nutrition.infrastructure.web.CreateAlimentRequest;
import com.dairy.apipinal.nutrition.infrastructure.web.CreatePrixAlimentRequest;
import com.dairy.apipinal.nutrition.infrastructure.web.CreateRationRequest;
import com.dairy.apipinal.production.domain.TypeTraite;
import com.dairy.apipinal.production.infrastructure.web.RecordMilkingRequest;
import com.dairy.apipinal.production.infrastructure.web.StartLactationRequest;
import com.dairy.apipinal.reproduction.infrastructure.web.ConstatGestationRequest;
import com.dairy.apipinal.reproduction.infrastructure.web.InseminationRequest;
import com.dairy.apipinal.reproduction.infrastructure.web.VelageRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public class FrontendRealityProofGateIT {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RaceRepository raceRepository;

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

        // 1. Insert Tenant in real PostgreSQL
        jdbcTemplate.update(
                "INSERT INTO tenant (id, nom, statut, created_at, updated_at, version) VALUES (?, 'Tenant Reality Gate', 'ACTIF', NOW(), NOW(), 0)",
                tenantId
        );

        // 2. Insert Exploitation in real PostgreSQL
        jdbcTemplate.update(
                "INSERT INTO exploitation (id, tenant_id, nom, localite, actif, created_at, updated_at, version) VALUES (?, ?, 'Ferme Reality Gate', 'Dakar', true, NOW(), NOW(), 0)",
                exploitationId, tenantId
        );

        // 3. Insert Utilisateur in real PostgreSQL
        jdbcTemplate.update(
                "INSERT INTO utilisateur (id, nom, prenom, statut, telephone, created_at, updated_at, version) VALUES (?, 'Reality', 'Tester', 'ACTIF', ?, NOW(), NOW(), 0)",
                userId, "+22177" + (System.currentTimeMillis() % 10000000L)
        );

        // 4. Insert Membership as PROPRIETAIRE
        jdbcTemplate.update(
                "INSERT INTO membership (id, utilisateur_id, exploitation_id, role, created_at, updated_at, version) VALUES (?, ?, ?, 'PROPRIETAIRE', NOW(), NOW(), 0)",
                UUID.randomUUID(), userId, exploitationId
        );

        // 5. Fetch valid race ID
        raceId = raceRepository.findAll().stream().findFirst().orElseThrow().getId();

        // 6. Generate real Bearer JWT
        jwtToken = jwtTokenProvider.generateAccessToken(userId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("PARCOURS 1 - ANIMAL: Création UI -> API -> DB -> GET -> Direct DB Edit -> GET -> Sortie")
    void testAnimalDomainTruth() throws Exception {
        String identifiant = "COW-" + System.currentTimeMillis();

        // 1. UI Create Animal (POST /api/v1/animals)
        CreateAnimalRequest createRequest = new CreateAnimalRequest(
                exploitationId,
                raceId,
                identifiant,
                "Vache Pinal Alpha",
                null,
                LocalDate.of(2022, 5, 10),
                null,
                null,
                SexeAnimal.FEMELLE
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/animals")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.identifiant").value(identifiant))
                .andExpect(jsonPath("$.nom").value("Vache Pinal Alpha"))
                .andExpect(jsonPath("$.sexe").value("FEMELLE"))
                .andExpect(jsonPath("$.statut").value("ACTIF"))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID animalId = UUID.fromString(jsonNode.get("id").asText());

        // 2. Truth check in PostgreSQL DB columns
        Map<String, Object> dbRow = jdbcTemplate.queryForMap(
                "SELECT nom, identifiant, sexe, statut, exploitation_id, tenant_id FROM animal WHERE id = ?",
                animalId
        );
        assertEquals("Vache Pinal Alpha", dbRow.get("nom"));
        assertEquals(identifiant, dbRow.get("identifiant"));
        assertEquals("FEMELLE", dbRow.get("sexe"));
        assertEquals("ACTIF", dbRow.get("statut"));
        assertEquals(exploitationId, dbRow.get("exploitation_id"));
        assertEquals(tenantId, dbRow.get("tenant_id"));

        // 3. UI Reload / GET /api/v1/animals/{id}
        mockMvc.perform(get("/api/v1/animals/" + animalId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(animalId.toString()))
                .andExpect(jsonPath("$.nom").value("Vache Pinal Alpha"))
                .andExpect(jsonPath("$.statut").value("ACTIF"));

        // 4. Test of Bidirectional Truth: Direct DB modification -> GET API -> UI reflects DB change
        jdbcTemplate.update("UPDATE animal SET nom = 'Vache Modifiee En DB' WHERE id = ?", animalId);

        mockMvc.perform(get("/api/v1/animals/" + animalId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Vache Modifiee En DB"));

        // 5. Declare Sortie (POST /api/v1/animals/{id}/sortie) - transfers ACTIF -> VENDU
        MouvementSortieRequest sortieRequest = new MouvementSortieRequest(
                LocalDate.now(),
                "VENTE",
                BigDecimal.valueOf(750000.0)
        );

        mockMvc.perform(post("/api/v1/animals/" + animalId + "/sortie")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sortieRequest)))
                .andExpect(status().isOk());

        // Verify DB status is VENDU
        String finalStatusInDb = jdbcTemplate.queryForObject(
                "SELECT statut FROM animal WHERE id = ?",
                String.class,
                animalId
        );
        assertEquals("VENDU", finalStatusInDb);

        // 6. Test Change Status directly on a second animal
        UUID cow2Id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO animal (id, tenant_id, exploitation_id, race_id, identifiant, nom, sexe, statut, date_naissance, created_at, updated_at, version) " +
                        "VALUES (?, ?, ?, ?, ?, 'Vache Statut Test', 'FEMELLE', 'ACTIF', '2022-01-01', NOW(), NOW(), 0)",
                cow2Id, tenantId, exploitationId, raceId, "COW-ST-" + System.currentTimeMillis()
        );

        mockMvc.perform(patch("/api/v1/animals/" + cow2Id + "/status?status=DECEDE")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("DECEDE"));

        String cow2Status = jdbcTemplate.queryForObject("SELECT statut FROM animal WHERE id = ?", String.class, cow2Id);
        assertEquals("DECEDE", cow2Status);
    }

    @Test
    @DisplayName("PARCOURS 2 - EXPLOITATION: Création UI -> API -> DB -> GET Active")
    void testExploitationDomainTruth() throws Exception {
        CreateExploitationRequest request = new CreateExploitationRequest(
                "Ferme Nouvelle Generation",
                "Thies Zone 4"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/exploitations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exploitationId").isNotEmpty())
                .andExpect(jsonPath("$.nom").value("Ferme Nouvelle Generation"))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID newExpId = UUID.fromString(jsonNode.get("exploitationId").asText());

        // Check DB
        Map<String, Object> expRow = jdbcTemplate.queryForMap(
                "SELECT nom, localite, actif FROM exploitation WHERE id = ?",
                newExpId
        );
        assertEquals("Ferme Nouvelle Generation", expRow.get("nom"));
        assertEquals("Thies Zone 4", expRow.get("localite"));
        assertTrue((Boolean) expRow.get("actif"));

        // Verify GET User Exploitations contains the newly created exploitation
        mockMvc.perform(get("/api/v1/exploitations")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.exploitationId == '" + newExpId + "')].nom").value("Ferme Nouvelle Generation"));
    }

    @Test
    @DisplayName("PARCOURS 3 & 4 - PRODUCTION, LACTATION & QUALITÉ DU LAIT: Start Lactation -> Milking -> Analysis -> GET")
    void testProductionAndLactationReality() throws Exception {
        // 1. Create cow in DB
        UUID cowId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO animal (id, tenant_id, exploitation_id, race_id, identifiant, nom, sexe, statut, date_naissance, created_at, updated_at, version) " +
                        "VALUES (?, ?, ?, ?, ?, 'Vache Laitiere', 'FEMELLE', 'ACTIF', '2021-01-01', NOW(), NOW(), 0)",
                cowId, tenantId, exploitationId, raceId, "COW-LACT-" + System.currentTimeMillis()
        );

        // 2. Start Lactation (POST /api/v1/lactations)
        StartLactationRequest startReq = new StartLactationRequest(cowId, LocalDate.of(2026, 1, 15));
        MvcResult lactResult = mockMvc.perform(post("/api/v1/lactations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(startReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.statut").value("EN_COURS"))
                .andReturn();

        UUID lactationId = UUID.fromString(objectMapper.readTree(lactResult.getResponse().getContentAsString()).get("id").asText());

        // Verify DB lactation
        Map<String, Object> lactRow = jdbcTemplate.queryForMap(
                "SELECT statut, date_debut FROM lactation WHERE id = ?",
                lactationId
        );
        assertEquals("EN_COURS", lactRow.get("statut"));

        // 3. UI query: GET Active Lactation for Cow
        mockMvc.perform(get("/api/v1/lactations/animals/" + cowId + "/active")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lactationId.toString()))
                .andExpect(jsonPath("$.statut").value("EN_COURS"));

        // 4. Record Milking (POST /api/v1/lactations/{lactationId}/milkings)
        RecordMilkingRequest milkingReq = new RecordMilkingRequest(
                OffsetDateTime.now(),
                TypeTraite.MATIN,
                BigDecimal.valueOf(14.5)
        );

        mockMvc.perform(post("/api/v1/lactations/" + lactationId + "/milkings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(milkingReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantiteKg").value(14.5));

        // Verify DB traite (column quantite_kg)
        BigDecimal litresInDb = jdbcTemplate.queryForObject(
                "SELECT quantite_kg FROM traite WHERE lactation_id = ? ORDER BY date_heure DESC LIMIT 1",
                BigDecimal.class,
                lactationId
        );
        assertNotNull(litresInDb);
        assertEquals(0, BigDecimal.valueOf(14.5).compareTo(litresInDb));

        // 5. Record Milk Analysis (POST /api/v1/lactations/{lactationId}/analyses)
        Map<String, Object> analysisReq = Map.of(
                "dateAnalyse", LocalDate.now().toString(),
                "tauxButyreux", 39.2,
                "tauxProteique", 33.5,
                "cellulesSomatiques", 120000
        );

        mockMvc.perform(post("/api/v1/lactations/" + lactationId + "/analyses")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(analysisReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tauxButyreux").value(39.2))
                .andExpect(jsonPath("$.cellulesSomatiques").value(120000));

        // 6. UI fetch Milk Analyses (GET /api/v1/lactations/{lactationId}/analyses)
        mockMvc.perform(get("/api/v1/lactations/" + lactationId + "/analyses")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tauxButyreux").value(39.2))
                .andExpect(jsonPath("$[0].tauxProteique").value(33.5));
    }

    @Test
    @DisplayName("PARCOURS 5 - REPRODUCTION: Insemination (Straw Code) -> Constat -> Vêlage")
    void testReproductionDomainTruth() throws Exception {
        UUID cowId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO animal (id, tenant_id, exploitation_id, race_id, identifiant, nom, sexe, statut, date_naissance, created_at, updated_at, version) " +
                        "VALUES (?, ?, ?, ?, ?, 'Genisse Repro', 'FEMELLE', 'ACTIF', '2023-03-01', NOW(), NOW(), 0)",
                cowId, tenantId, exploitationId, raceId, "COW-REPRO-" + System.currentTimeMillis()
        );

        // 1. Declare Insemination with codePaillette (POST /api/v1/reproduction/inseminations)
        InseminationRequest insemReq = new InseminationRequest(
                cowId,
                exploitationId,
                LocalDate.now().minusMonths(9),
                "INSEMINATION_ARTIFICIELLE",
                null,
                "STRAW-MONBE-88"
        );

        MvcResult insemResult = mockMvc.perform(post("/api/v1/reproduction/inseminations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insemReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_CONSTAT"))
                .andReturn();

        UUID cycleId = UUID.fromString(objectMapper.readTree(insemResult.getResponse().getContentAsString()).get("id").asText());

        // Verify DB cycle_reproduction table for straw code
        String strawCodeInDb = jdbcTemplate.queryForObject(
                "SELECT code_paillette FROM cycle_reproduction WHERE id = ?",
                String.class,
                cycleId
        );
        assertEquals("STRAW-MONBE-88", strawCodeInDb);

        // 2. Enregistrer Constat de Gestation (POST /api/v1/reproduction/cycles/{cycleId}/constat)
        ConstatGestationRequest constatReq = new ConstatGestationRequest(
                LocalDate.now().minusMonths(7),
                "POSITIF",
                "Dr Ndiaye"
        );

        mockMvc.perform(post("/api/v1/reproduction/cycles/" + cycleId + "/constat")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(constatReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("GESTANTE"));

        // Verify DB cycle status
        String cycleStatusInDb = jdbcTemplate.queryForObject(
                "SELECT statut FROM cycle_reproduction WHERE id = ?",
                String.class,
                cycleId
        );
        assertEquals("GESTANTE", cycleStatusInDb);

        // 3. Déclarer Vêlage (POST /api/v1/reproduction/cycles/{cycleId}/velage)
        VelageRequest velageReq = new VelageRequest(
                LocalDate.now(),
                List.of(new VelageRequest.VeauPayload("CALF-TRUTH-01", "Veau Alpha", "FEMELLE", 1))
        );

        mockMvc.perform(post("/api/v1/reproduction/cycles/" + cycleId + "/velage")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(velageReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("TERMINEE_VELAGE"));

        // Verify calf created in DB (wait for async event listener)
        int calfCount = 0;
        for (int i = 0; i < 20; i++) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM animal WHERE identifiant = 'CALF-TRUTH-01'",
                    Integer.class
            );
            if (count != null && count > 0) {
                calfCount = count;
                break;
            }
            Thread.sleep(100);
        }
        assertTrue(calfCount >= 1);
    }

    @Test
    @DisplayName("PARCOURS 6 - NUTRITION: Aliment -> Prix -> Ration -> Ligne -> Activation -> Coût")
    void testNutritionDomainTruth() throws Exception {
        // 1. Create Aliment (POST /api/v1/aliments)
        String codeAlim = "ALIM-" + System.currentTimeMillis();
        CreateAlimentRequest createAlim = new CreateAlimentRequest(
                codeAlim,
                "Tourteau de Coton Reality",
                "CONCENTRE",
                UniteAliment.KG
        );

        MvcResult alimResult = mockMvc.perform(post("/api/v1/aliments")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAlim)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nom").value("Tourteau de Coton Reality"))
                .andReturn();

        UUID alimentId = UUID.fromString(objectMapper.readTree(alimResult.getResponse().getContentAsString()).get("id").asText());

        // Verify DB aliment
        String nomInDb = jdbcTemplate.queryForObject(
                "SELECT nom FROM aliment WHERE id = ?",
                String.class,
                alimentId
        );
        assertEquals("Tourteau de Coton Reality", nomInDb);

        // 2. Set Aliment Price (POST /api/v1/aliments/{alimentId}/prices)
        CreatePrixAlimentRequest prixReq = new CreatePrixAlimentRequest(
                BigDecimal.valueOf(275.50),
                LocalDate.now().minusDays(15),
                null
        );

        mockMvc.perform(post("/api/v1/aliments/" + alimentId + "/prices")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prixReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixUnitaire").value(275.50));

        // 3. Create Cow & Ration
        UUID cowId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO animal (id, tenant_id, exploitation_id, race_id, identifiant, nom, sexe, statut, date_naissance, created_at, updated_at, version) " +
                        "VALUES (?, ?, ?, ?, ?, 'Vache Nutri', 'FEMELLE', 'ACTIF', '2022-02-02', NOW(), NOW(), 0)",
                cowId, tenantId, exploitationId, raceId, "COW-NUTRI-" + System.currentTimeMillis()
        );

        CreateRationRequest rationReq = new CreateRationRequest(
                LocalDate.now(),
                OrigineRation.ACTUELLE
        );

        MvcResult rationResult = mockMvc.perform(post("/api/v1/animals/" + cowId + "/rations")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rationReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        UUID rationId = UUID.fromString(objectMapper.readTree(rationResult.getResponse().getContentAsString()).get("id").asText());

        // 4. Add Line to Ration (POST /api/v1/animals/{cowId}/rations/{rationId}/lines)
        AddRationLineRequest lineReq = new AddRationLineRequest(alimentId, BigDecimal.valueOf(4.0));

        mockMvc.perform(post("/api/v1/animals/" + cowId + "/rations/" + rationId + "/lines")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lineReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes").isArray());

        // 5. Activate Ration (POST /api/v1/animals/{cowId}/rations/{rationId}/activate)
        mockMvc.perform(post("/api/v1/animals/" + cowId + "/rations/" + rationId + "/activate")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ACTIVE"));

        // 6. Calculate Ration Cost (GET /api/v1/animals/{cowId}/rations/{rationId}/cost?date=...)
        mockMvc.perform(get("/api/v1/animals/" + cowId + "/rations/" + rationId + "/cost?date=" + LocalDate.now())
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coutTotal").isNotEmpty());
    }

    @Test
    @DisplayName("PARCOURS 7 - SANTÉ: Enregistrement -> Liste -> Résolution (dateFin) -> DB Edit -> GET")
    void testHealthDomainTruth() throws Exception {
        UUID cowId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO animal (id, tenant_id, exploitation_id, race_id, identifiant, nom, sexe, statut, date_naissance, created_at, updated_at, version) " +
                        "VALUES (?, ?, ?, ?, ?, 'Vache Sante', 'FEMELLE', 'ACTIF', '2022-04-04', NOW(), NOW(), 0)",
                cowId, tenantId, exploitationId, raceId, "COW-SANTE-" + System.currentTimeMillis()
        );

        // 1. Declare Health Event (POST /api/v1/animals/{cowId}/health-events)
        RecordHealthEventRequest eventReq = new RecordHealthEventRequest(
                OffsetDateTime.now(),
                "Boiterie du pied gauche"
        );

        MvcResult eventResult = mockMvc.perform(post("/api/v1/animals/" + cowId + "/health-events")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.description").value("Boiterie du pied gauche"))
                .andReturn();

        UUID eventId = UUID.fromString(objectMapper.readTree(eventResult.getResponse().getContentAsString()).get("id").asText());

        // Verify DB event
        String descInDb = jdbcTemplate.queryForObject(
                "SELECT description FROM evenement_sanitaire WHERE id = ?",
                String.class,
                eventId
        );
        assertEquals("Boiterie du pied gauche", descInDb);

        // 2. Resolve Alert / Event with dateFin (PATCH /api/v1/animals/{cowId}/health-events/{eventId})
        UpdateHealthEventRequest updateReq = new UpdateHealthEventRequest(
                "Boiterie en voie de guerison",
                "Boiterie soignee",
                "Fin traitement",
                LocalDate.now()
        );

        mockMvc.perform(patch("/api/v1/animals/" + cowId + "/health-events/" + eventId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnostic").value("Boiterie soignee"))
                .andExpect(jsonPath("$.dateFin").value(LocalDate.now().toString()));

        // Verify DB date_fin column
        LocalDate dateFinInDb = jdbcTemplate.queryForObject(
                "SELECT date_fin FROM evenement_sanitaire WHERE id = ?",
                LocalDate.class,
                eventId
        );
        assertEquals(LocalDate.now(), dateFinInDb);

        // 3. Direct DB modification -> GET API reflections
        jdbcTemplate.update("UPDATE evenement_sanitaire SET diagnostic = 'Diagnostic Modifie En DB' WHERE id = ?", eventId);

        mockMvc.perform(get("/api/v1/animals/" + cowId + "/health-events/" + eventId)
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diagnostic").value("Diagnostic Modifie En DB"));
    }

    @Test
    @DisplayName("PARCOURS 8 - FINANCE: Fixation Prix Vente -> Création Charge -> Dashboard Query")
    void testFinanceDomainTruth() throws Exception {
        // 1. Set Milk Selling Price (POST /api/v1/finance/prix-vente)
        PrixVenteRequest prixReq = new PrixVenteRequest(
                BigDecimal.valueOf(550.0),
                LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(6)
        );

        mockMvc.perform(post("/api/v1/finance/prix-vente")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prixReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prixParLitre").value(550.0));

        // Verify DB prix_vente_lait (column prix_par_litre)
        BigDecimal prixInDb = jdbcTemplate.queryForObject(
                "SELECT prix_par_litre FROM prix_vente_lait WHERE tenant_id = ? ORDER BY date_debut DESC LIMIT 1",
                BigDecimal.class,
                tenantId
        );
        assertNotNull(prixInDb);
        assertEquals(0, BigDecimal.valueOf(550.0).compareTo(prixInDb));

        // 2. Create Charge (POST /api/v1/finance/charges)
        ChargeRequest chargeReq = new ChargeRequest(
                "Achat fourrage foin",
                "ALIMENTATION",
                BigDecimal.valueOf(45000.0),
                LocalDate.now(),
                "25 bottes de foin"
        );

        mockMvc.perform(post("/api/v1/finance/charges")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chargeReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montant").value(45000.0))
                .andExpect(jsonPath("$.libelle").value("Achat fourrage foin"));

        // Verify DB charge_exploitation
        BigDecimal chargeInDb = jdbcTemplate.queryForObject(
                "SELECT montant FROM charge_exploitation WHERE tenant_id = ? AND libelle = 'Achat fourrage foin'",
                BigDecimal.class,
                tenantId
        );
        assertNotNull(chargeInDb);
        assertEquals(0, BigDecimal.valueOf(45000.0).compareTo(chargeInDb));

        // 3. UI Query Dashboard (GET /api/v1/finance/dashboard)
        mockMvc.perform(get("/api/v1/finance/dashboard")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("X-Exploitation-ID", exploitationId.toString()))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis").exists())
                .andExpect(jsonPath("$.transactions").isArray());
    }
}
