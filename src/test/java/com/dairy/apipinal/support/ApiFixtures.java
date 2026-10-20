package com.dairy.apipinal.support;

import com.dairy.apipinal.identity.infrastructure.security.JwtTokenProvider;
import com.dairy.apipinal.shared.security.AuthenticationMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Outils communs aux tests d'intégration HTTP : création d'une exploitation et de son propriétaire
 * en base réelle, appels authentifiés (JWT + X-Exploitation-ID) et lecture des réponses.
 */
public final class ApiFixtures {

    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final JwtTokenProvider jwt;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    public final UUID tenantId = UUID.randomUUID();
    public final UUID exploitationId = UUID.randomUUID();
    public final UUID ownerId = UUID.randomUUID();
    public final UUID raceId;

    public ApiFixtures(WebApplicationContext context, JdbcTemplate jdbc, JwtTokenProvider jwt) {
        this.mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        this.jdbc = jdbc;
        this.jwt = jwt;
        this.raceId = jdbc.queryForObject("SELECT id FROM race ORDER BY code LIMIT 1", UUID.class);
        jdbc.update("INSERT INTO tenant (id, nom, statut, created_at, updated_at, version) VALUES (?, 'Tenant', 'ACTIF', NOW(), NOW(), 0)", tenantId);
        jdbc.update("INSERT INTO exploitation (id, tenant_id, nom, localite, actif, created_at, updated_at, version) "
                + "VALUES (?, ?, 'Ferme test', 'Thiès', true, NOW(), NOW(), 0)", exploitationId, tenantId);
        jdbc.update("INSERT INTO utilisateur (id, nom, prenom, statut, telephone, created_at, updated_at, version) "
                + "VALUES (?, 'Diallo', 'Awa', 'ACTIF', ?, NOW(), NOW(), 0)", ownerId, uniquePhone());
        jdbc.update("INSERT INTO membership (id, utilisateur_id, exploitation_id, role, created_at, updated_at, version) "
                + "VALUES (?, ?, ?, 'PROPRIETAIRE', NOW(), NOW(), 0)", UUID.randomUUID(), ownerId, exploitationId);
    }

    /** Appel du propriétaire (session OTP) dans l'exploitation de test. */
    public ResultActions owner(MockHttpServletRequestBuilder request, Object body) throws Exception {
        request.header("Authorization", "Bearer " + jwt.generateAccessToken(ownerId, AuthenticationMethod.OTP));
        request.header("X-Exploitation-ID", exploitationId.toString());
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(request);
    }

    public UUID createAnimal(String identifiant, String sexe) throws Exception {
        return id(owner(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/animals"), Map.of(
                "raceId", raceId, "identifiant", identifiant, "nom", "Animal " + identifiant, "sexe", sexe,
                "dateNaissance", LocalDate.now().minusYears(4).toString()
        )).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    public JsonNode body(ResultActions actions) throws Exception {
        return json.readTree(actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    public UUID id(ResultActions actions) throws Exception {
        return UUID.fromString(body(actions).get("id").asText());
    }

    private UUID id(String content) throws Exception {
        return UUID.fromString(json.readTree(content).get("id").asText());
    }

    public JdbcTemplate jdbc() {
        return jdbc;
    }

    public static String uniquePhone() {
        return "+2217" + String.format("%08d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000L));
    }
}
