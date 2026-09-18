package com.dairy.apipinal.nutrition;

import com.dairy.apipinal.nutrition.application.GetApplicablePrixAliment;
import com.dairy.apipinal.nutrition.application.GetPrixAlimentHistory;
import com.dairy.apipinal.nutrition.application.PrixAlimentService;
import com.dairy.apipinal.nutrition.application.CreatePrixAliment;
import com.dairy.apipinal.nutrition.domain.PrixAliment;
import com.dairy.apipinal.nutrition.infrastructure.web.PrixAlimentController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PrixAlimentControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private PrixAlimentService prixAlimentService;

    private UUID alimentId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new PrixAlimentController(prixAlimentService)
                )
                .build();

        objectMapper = new ObjectMapper();

        alimentId = UUID.randomUUID();
    }

    @Test
    void shouldCreatePrixAliment() throws Exception {

        PrixAliment prixAliment = new PrixAliment(
                alimentId,
                new BigDecimal("125.00"),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(prixAlimentService.create(
                any(CreatePrixAliment.class)
        )).thenReturn(prixAliment);

        String requestBody = """
                {
                  "prixUnitaire": 125.00,
                  "dateDebut": "2026-09-01",
                  "dateFin": "2026-09-30"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/v1/aliments/{alimentId}/prices",
                                alimentId
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
                        jsonPath("$.alimentId")
                                .value(alimentId.toString())
                )
                .andExpect(
                        jsonPath("$.prixUnitaire")
                                .value(125.0)
                )
                .andExpect(
                        jsonPath("$.dateDebut")
                                .value("2026-09-01")
                )
                .andExpect(
                        jsonPath("$.dateFin")
                                .value("2026-09-30")
                );

        verify(prixAlimentService)
                .create(any(CreatePrixAliment.class));
    }

    @Test
    void shouldRejectMissingPrice() throws Exception {

        String requestBody = """
            {
              "dateDebut": "2026-09-01",
              "dateFin": "2026-09-30"
            }
            """;

        mockMvc.perform(
                        post(
                                "/api/v1/aliments/{alimentId}/prices",
                                alimentId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNonPositivePrice() throws Exception {

        String requestBody = """
            {
              "prixUnitaire": 0,
              "dateDebut": "2026-09-01",
              "dateFin": "2026-09-30"
            }
            """;

        mockMvc.perform(
                        post(
                                "/api/v1/aliments/{alimentId}/prices",
                                alimentId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateOpenEndedPrixAliment() throws Exception {

        PrixAliment prixAliment = new PrixAliment(
                alimentId,
                new BigDecimal("150.00"),
                LocalDate.of(2026, 10, 1),
                null
        );

        when(prixAlimentService.create(
                any(CreatePrixAliment.class)
        )).thenReturn(prixAliment);

        String requestBody = """
            {
              "prixUnitaire": 150.00,
              "dateDebut": "2026-10-01"
            }
            """;

        mockMvc.perform(
                        post(
                                "/api/v1/aliments/{alimentId}/prices",
                                alimentId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.prixUnitaire")
                                .value(150.0)
                )
                .andExpect(
                        jsonPath("$.dateDebut")
                                .value("2026-10-01")
                )
                .andExpect(
                        jsonPath("$.dateFin")
                                .doesNotExist()
                );
    }

    @Test
    void shouldGetPrixAlimentHistory() throws Exception {

        PrixAliment prixOctobre = new PrixAliment(
                alimentId,
                new BigDecimal("120.00"),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)
        );

        PrixAliment prixSeptembre = new PrixAliment(
                alimentId,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(prixAlimentService.getHistory(
                new GetPrixAlimentHistory(alimentId)
        )).thenReturn(
                List.of(
                        prixOctobre,
                        prixSeptembre
                )
        );

        mockMvc.perform(
                        get(
                                "/api/v1/aliments/{alimentId}/prices",
                                alimentId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].alimentId")
                                .value(alimentId.toString())
                )
                .andExpect(
                        jsonPath("$[0].prixUnitaire")
                                .value(120.0)
                )
                .andExpect(
                        jsonPath("$[0].dateDebut")
                                .value("2026-10-01")
                )
                .andExpect(
                        jsonPath("$[1].prixUnitaire")
                                .value(100.0)
                )
                .andExpect(
                        jsonPath("$[1].dateDebut")
                                .value("2026-09-01")
                );

        ArgumentCaptor<GetPrixAlimentHistory> captor =
                ArgumentCaptor.forClass(
                        GetPrixAlimentHistory.class
                );

        verify(prixAlimentService)
                .getHistory(captor.capture());

        assertThat(captor.getValue().alimentId())
                .isEqualTo(alimentId);
    }

    @Test
    void shouldGetApplicablePrice() throws Exception {

        UUID prixId = UUID.randomUUID();

        LocalDate date = LocalDate.of(2026, 10, 15);

        PrixAliment prix = new PrixAliment(
                alimentId,
                new BigDecimal("120.00"),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)
        );

        when(prixAlimentService.getApplicablePrice(
                new GetApplicablePrixAliment(
                        alimentId,
                        date
                )
        )).thenReturn(Optional.of(prix));

        mockMvc.perform(
                        get(
                                "/api/v1/aliments/{alimentId}/prices/applicable",
                                alimentId
                        )
                                .param("date", "2026-10-15")
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.alimentId")
                                .value(alimentId.toString())
                )
                .andExpect(
                        jsonPath("$.prixUnitaire")
                                .value(120.0)
                )
                .andExpect(
                        jsonPath("$.dateDebut")
                                .value("2026-10-01")
                )
                .andExpect(
                        jsonPath("$.dateFin")
                                .value("2026-10-31")
                );

        ArgumentCaptor<GetApplicablePrixAliment> captor =
                ArgumentCaptor.forClass(
                        GetApplicablePrixAliment.class
                );

        verify(prixAlimentService)
                .getApplicablePrice(captor.capture());

        assertThat(captor.getValue().alimentId())
                .isEqualTo(alimentId);

        assertThat(captor.getValue().date())
                .isEqualTo(date);
    }

    @Test
    void shouldReturnNotFoundWhenNoApplicablePrice() throws Exception {

        LocalDate date = LocalDate.of(2025, 12, 15);

        when(prixAlimentService.getApplicablePrice(
                new GetApplicablePrixAliment(
                        alimentId,
                        date
                )
        )).thenReturn(Optional.empty());

        mockMvc.perform(
                        get(
                                "/api/v1/aliments/{alimentId}/prices/applicable",
                                alimentId
                        )
                                .param("date", "2025-12-15")
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectMissingDate() throws Exception {

        mockMvc.perform(
                        get(
                                "/api/v1/aliments/{alimentId}/prices/applicable",
                                alimentId
                        )
                )
                .andExpect(status().isBadRequest());
    }
}