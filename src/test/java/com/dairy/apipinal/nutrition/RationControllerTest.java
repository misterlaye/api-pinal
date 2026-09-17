package com.dairy.apipinal.nutrition;

import com.dairy.apipinal.nutrition.application.*;
import com.dairy.apipinal.nutrition.domain.OrigineRation;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.infrastructure.web.RationController;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void shouldTerminateRation() throws Exception {

        UUID rationId = UUID.randomUUID();
        UUID alimentId = UUID.randomUUID();

        Ration ration = new Ration(
                tenantId,
                animalId,
                LocalDate.of(2026, 1, 1),
                OrigineRation.ACTUELLE
        );

        ration.ajouterLigne(
                alimentId,
                new java.math.BigDecimal("5")
        );

        ration.activer();

        LocalDate dateFin = LocalDate.of(2026, 1, 31);

        ration.terminer(dateFin);

        when(rationService.terminate(any(TerminateRation.class)))
                .thenReturn(ration);

        String requestBody = """
            {
              "dateFin": "2026-01-31"
            }
            """;

        mockMvc.perform(
                        post(
                                "/api/v1/animals/{animalId}/rations/{rationId}/terminate",
                                animalId,
                                rationId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.animalId")
                                .value(animalId.toString())
                )
                .andExpect(
                        jsonPath("$.tenantId")
                                .value(tenantId.toString())
                )
                .andExpect(
                        jsonPath("$.dateDebut")
                                .value("2026-01-01")
                )
                .andExpect(
                        jsonPath("$.dateFin")
                                .value("2026-01-31")
                )
                .andExpect(
                        jsonPath("$.statut")
                                .value("TERMINEE")
                )
                .andExpect(
                        jsonPath("$.origine")
                                .value("ACTUELLE")
                );
    }

    @Test
    void shouldGetRation() throws Exception {

        UUID rationId = UUID.randomUUID();
        UUID alimentId = UUID.randomUUID();

        Ration ration = new Ration(
                tenantId,
                animalId,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        ration.ajouterLigne(
                alimentId,
                new java.math.BigDecimal("5")
        );

        when(rationService.getRation(
                new com.dairy.apipinal.nutrition.application.GetRation(rationId)
        )).thenReturn(ration);

        mockMvc.perform(
                        get(
                                "/api/v1/animals/{animalId}/rations/{rationId}",
                                animalId,
                                rationId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.tenantId")
                                .value(tenantId.toString())
                )
                .andExpect(
                        jsonPath("$.animalId")
                                .value(animalId.toString())
                )
                .andExpect(
                        jsonPath("$.dateDebut")
                                .value("2026-09-01")
                )
                .andExpect(
                        jsonPath("$.statut")
                                .value("BROUILLON")
                )
                .andExpect(
                        jsonPath("$.origine")
                                .value("ACTUELLE")
                )
                .andExpect(
                        jsonPath("$.lignes.length()")
                                .value(1)
                );
    }

    @Test
    void shouldRejectRationFromAnotherAnimal() throws Exception {

        UUID rationId = UUID.randomUUID();
        UUID requestedAnimalId = UUID.randomUUID();
        UUID rationAnimalId = UUID.randomUUID();

        Ration ration = new Ration(
                tenantId,
                rationAnimalId,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        when(rationService.getRation(
                new com.dairy.apipinal.nutrition.application.GetRation(rationId)
        )).thenReturn(ration);

        mockMvc.perform(
                        get(
                                "/api/v1/animals/{animalId}/rations/{rationId}",
                                requestedAnimalId,
                                rationId
                        )
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetRationsByAnimal() throws Exception {

        UUID rationId = UUID.randomUUID();
        UUID alimentId = UUID.randomUUID();

        Ration ration = new Ration(
                tenantId,
                animalId,
                LocalDate.of(2026, 9, 16),
                OrigineRation.ACTUELLE
        );

        ration.ajouterLigne(
                alimentId,
                new java.math.BigDecimal("5")
        );

        var pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "dateDebut")
        );

        var page = new PageImpl<>(
                List.of(ration),
                pageable,
                1
        );

        when(rationService.getRationsByAnimal(
                any(GetRationsByAnimal.class)
        )).thenReturn(page);

        mockMvc.perform(
                        get(
                                "/api/v1/animals/{animalId}/rations",
                                animalId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].tenantId")
                                .value(tenantId.toString())
                )
                .andExpect(
                        jsonPath("$.content[0].animalId")
                                .value(animalId.toString())
                )
                .andExpect(
                        jsonPath("$.content[0].dateDebut")
                                .value("2026-09-16")
                )
                .andExpect(
                        jsonPath("$.content[0].statut")
                                .value("BROUILLON")
                )
                .andExpect(
                        jsonPath("$.content[0].origine")
                                .value("ACTUELLE")
                )
                .andExpect(
                        jsonPath("$.content[0].lignes.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.page")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.size")
                                .value(20)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.totalPages")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.first")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.last")
                                .value(true)
                );

        verify(rationService).getRationsByAnimal(
                any(GetRationsByAnimal.class)
        );
    }

    @Test
    void shouldGetRationsWithPaginationParameters() throws Exception {

        var pageable = PageRequest.of(
                1,
                10,
                Sort.by(Sort.Direction.DESC, "dateDebut")
        );

        var page = new PageImpl<Ration>(
                List.of(),
                pageable,
                15
        );

        when(rationService.getRationsByAnimal(
                any(GetRationsByAnimal.class)
        )).thenReturn(page);

        mockMvc.perform(
                        get(
                                "/api/v1/animals/{animalId}/rations",
                                animalId
                        )
                                .param("page", "1")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(0)
                )
                .andExpect(
                        jsonPath("$.page")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.size")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(15)
                )
                .andExpect(
                        jsonPath("$.totalPages")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.first")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.last")
                                .value(true)
                );

        ArgumentCaptor<GetRationsByAnimal> captor =
                ArgumentCaptor.forClass(GetRationsByAnimal.class);

        verify(rationService).getRationsByAnimal(captor.capture());

        GetRationsByAnimal query = captor.getValue();

        assertThat(query.animalId())
                .isEqualTo(animalId);

        assertThat(query.pageable().getPageNumber())
                .isEqualTo(1);

        assertThat(query.pageable().getPageSize())
                .isEqualTo(10);

        assertThat(query.pageable().getSort())
                .isEqualTo(
                        Sort.by(
                                Sort.Direction.DESC,
                                "dateDebut"
                        )
                );
    }

    @Test
    void shouldRejectNegativePage() throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/animals/{animalId}/rations",
                                animalId
                        )
                                .param("page", "-1")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidPageSize() throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/animals/{animalId}/rations",
                                animalId
                        )
                                .param("size", "101")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCalculateRationCost() throws Exception {

        UUID rationId = UUID.randomUUID();
        UUID alimentId = UUID.randomUUID();

        LocalDate dateCalcul = LocalDate.of(2026, 9, 17);

        RationCostResult result = new RationCostResult(
                rationId,
                dateCalcul,
                List.of(
                        new RationCostLine(
                                alimentId,
                                new BigDecimal("5.0000"),
                                new BigDecimal("100.00"),
                                new BigDecimal("500.00")
                        )
                ),
                new BigDecimal("500.00")
        );

        when(rationService.calculateCost(
                any(CalculateRationCost.class)
        )).thenReturn(result);

        mockMvc.perform(
                        get(
                                "/api/v1/animals/{animalId}/rations/{rationId}/cost",
                                animalId,
                                rationId
                        )
                                .param("date", "2026-09-17")
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.rationId")
                                .value(rationId.toString())
                )
                .andExpect(
                        jsonPath("$.dateCalcul")
                                .value("2026-09-17")
                )
                .andExpect(
                        jsonPath("$.lignes.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.lignes[0].alimentId")
                                .value(alimentId.toString())
                )
                .andExpect(
                        jsonPath("$.lignes[0].quantite")
                                .value(5.0)
                )
                .andExpect(
                        jsonPath("$.lignes[0].prixUnitaire")
                                .value(100.0)
                )
                .andExpect(
                        jsonPath("$.lignes[0].cout")
                                .value(500.0)
                )
                .andExpect(
                        jsonPath("$.coutTotal")
                                .value(500.0)
                );
        ArgumentCaptor<CalculateRationCost> captor =
                ArgumentCaptor.forClass(CalculateRationCost.class);

        verify(rationService).calculateCost(captor.capture());

        CalculateRationCost query = captor.getValue();

        assertThat(query.animalId())
                .isEqualTo(animalId);

        assertThat(query.rationId())
                .isEqualTo(rationId);

        assertThat(query.dateCalcul())
                .isEqualTo(dateCalcul);
    }

    @Test
    void shouldRejectCostCalculationWhenDateIsMissing() throws Exception {

        UUID rationId = UUID.randomUUID();

        mockMvc.perform(
                        get(
                                "/api/v1/animals/{animalId}/rations/{rationId}/cost",
                                animalId,
                                rationId
                        )
                )
                .andExpect(status().isBadRequest());
    }
}