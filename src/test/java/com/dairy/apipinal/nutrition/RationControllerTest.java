package com.dairy.apipinal.nutrition;

import com.dairy.apipinal.nutrition.application.RationService;
import com.dairy.apipinal.nutrition.domain.OrigineRation;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.domain.StatutRation;
import com.dairy.apipinal.nutrition.infrastructure.web.RationController;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RationControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private RationService rationService;

    private UUID animalId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders
                .standaloneSetup(new RationController(rationService))
                .build();

        objectMapper = new ObjectMapper();

        animalId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
    }

    @Test
    void shouldCreateDraftRation() throws Exception {

        Ration ration = new Ration(
                tenantId,
                animalId,
                LocalDate.of(2026, 9, 16),
                OrigineRation.ACTUELLE
        );

        /*
         * Avec GenerationType.UUID, l'ID est normalement fourni
         * par JPA. Ici le contrôleur reçoit une Ration provenant
         * du service mocké. Pour le test HTTP, l'identité exacte
         * n'est pas notre responsabilité.
         */
        when(rationService.create(any()))
                .thenReturn(ration);

        String requestBody = """
                {
                  "dateDebut": "2026-09-16",
                  "origine": "ACTUELLE"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/animals/{animalId}/rations", animalId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.tenantId")
                        .value(tenantId.toString()))
                .andExpect(jsonPath("$.animalId")
                        .value(animalId.toString()))
                .andExpect(jsonPath("$.dateDebut")
                        .value("2026-09-16"))
                .andExpect(jsonPath("$.statut")
                        .value("BROUILLON"))
                .andExpect(jsonPath("$.origine")
                        .value("ACTUELLE"));
    }

    @Test
    void shouldRejectMissingStartDate() throws Exception {

        String requestBody = """
                {
                  "origine": "ACTUELLE"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/animals/{animalId}/rations", animalId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectMissingOrigin() throws Exception {

        String requestBody = """
                {
                  "dateDebut": "2026-09-16"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/animals/{animalId}/rations", animalId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldActivateRation() throws Exception {

        UUID rationId = UUID.randomUUID();

        Ration ration = new Ration(
                tenantId,
                animalId,
                LocalDate.of(2026, 9, 16),
                OrigineRation.ACTUELLE
        );

        ration.ajouterLigne(
                UUID.randomUUID(),
                new java.math.BigDecimal("5")
        );

        ration.activer();

        when(rationService.activate(
                animalId,
                rationId
        )).thenReturn(ration);

        mockMvc.perform(
                        post(
                                "/api/v1/animals/{animalId}/rations/{rationId}/activate",
                                animalId,
                                rationId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.statut")
                                .value("ACTIVE")
                )
                .andExpect(
                        jsonPath("$.animalId")
                                .value(animalId.toString())
                );
    }
}