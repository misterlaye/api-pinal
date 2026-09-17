package com.dairy.apipinal.finance;

import com.dairy.apipinal.finance.api.AnimalRentabilityReference;
import com.dairy.apipinal.finance.api.FinanceQueries;
import com.dairy.apipinal.finance.api.RentabiliteReference;
import com.dairy.apipinal.finance.infrastructure.web.AnimalRentabilityResponse;
import com.dairy.apipinal.finance.infrastructure.web.FinanceController;
import com.dairy.apipinal.finance.infrastructure.web.FinanceRestMapper;
import com.dairy.apipinal.finance.infrastructure.web.RentabiliteResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FinanceController.class)
@WithMockUser
class FinanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinanceQueries financeQueries;

    @MockitoBean
    private FinanceRestMapper mapper;

    @Test
    void shouldReturnLatestRentabilite() throws Exception {

        UUID calculationId = UUID.randomUUID();

        RentabiliteReference reference =
                new RentabiliteReference(
                        calculationId,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30),
                        OffsetDateTime.parse(
                                "2026-09-30T18:00:00Z"
                        ),
                        new BigDecimal("1200.0000"),
                        new BigDecimal("800000.0000"),
                        new BigDecimal("250000.0000"),
                        new BigDecimal("150000.0000"),
                        new BigDecimal("400000.0000"),
                        new BigDecimal("333.3333"),
                        new BigDecimal("400000.0000")
                );

        RentabiliteResponse response =
                new RentabiliteResponse(
                        calculationId,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30),
                        OffsetDateTime.parse(
                                "2026-09-30T18:00:00Z"
                        ),
                        new BigDecimal("1200.0000"),
                        new BigDecimal("800000.0000"),
                        new BigDecimal("250000.0000"),
                        new BigDecimal("150000.0000"),
                        new BigDecimal("400000.0000"),
                        new BigDecimal("333.3333"),
                        new BigDecimal("400000.0000")
                );

        when(financeQueries.findLatestRentabilite())
                .thenReturn(Optional.of(reference));

        when(mapper.toResponse(reference))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/v1/finance/rentabilite/latest")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(calculationId.toString())
                )
                .andExpect(
                        jsonPath("$.periodeDebut")
                                .value("2026-09-01")
                )
                .andExpect(
                        jsonPath("$.periodeFin")
                                .value("2026-09-30")
                )
                .andExpect(
                        jsonPath("$.volumeLait")
                                .value(1200.0)
                )
                .andExpect(
                        jsonPath("$.coutTotal")
                                .value(400000.0)
                )
                .andExpect(
                        jsonPath("$.coutRevientParLitre")
                                .value(333.3333)
                )
                .andExpect(
                        jsonPath("$.marge")
                                .value(400000.0)
                );
    }

    @Test
    void shouldReturn404WhenNoRentabiliteExists() throws Exception {

        when(financeQueries.findLatestRentabilite())
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/v1/finance/rentabilite/latest")
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnAnimalRentability() throws Exception {

        UUID animalId = UUID.randomUUID();

        AnimalRentabilityReference reference =
                new AnimalRentabilityReference(
                        animalId,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30),
                        new BigDecimal("700.000"),
                        new BigDecimal("700.000"),
                        new BigDecimal("420000.0000"),
                        new BigDecimal("120000.0000"),
                        new BigDecimal("300000.0000"),
                        new BigDecimal("600.0000"),
                        new BigDecimal("171.4286"),
                        true
                );

        AnimalRentabilityResponse response =
                new AnimalRentabilityResponse(
                        animalId,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30),
                        new BigDecimal("700.000"),
                        new BigDecimal("700.000"),
                        new BigDecimal("420000.0000"),
                        new BigDecimal("120000.0000"),
                        new BigDecimal("300000.0000"),
                        new BigDecimal("600.0000"),
                        new BigDecimal("171.4286"),
                        true
                );

        when(
                financeQueries.calculateAnimalRentability(
                        animalId,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30)
                )
        ).thenReturn(Optional.of(reference));

        when(mapper.toResponse(reference))
                .thenReturn(response);

        mockMvc.perform(
                        get(
                                "/api/v1/finance/animals/{animalId}/rentabilite",
                                animalId
                        )
                                .param("dateDebut", "2026-09-01")
                                .param("dateFin", "2026-09-30")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.animalId")
                                .value(animalId.toString())
                )
                .andExpect(
                        jsonPath("$.volumeLaitKg")
                                .value(700.0)
                )
                .andExpect(
                        jsonPath("$.chiffreAffaires")
                                .value(420000.0)
                )
                .andExpect(
                        jsonPath("$.coutAlimentation")
                                .value(120000.0)
                )
                .andExpect(
                        jsonPath("$.marge")
                                .value(300000.0)
                )
                .andExpect(
                        jsonPath("$.rentable")
                                .value(true)
                );
    }

    @Test
    void shouldReturn404WhenAnimalRentabilityCannotBeCalculated()
            throws Exception {

        UUID animalId = UUID.randomUUID();

        when(
                financeQueries.calculateAnimalRentability(
                        animalId,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30)
                )
        ).thenReturn(Optional.empty());

        mockMvc.perform(
                        get(
                                "/api/v1/finance/animals/{animalId}/rentabilite",
                                animalId
                        )
                                .param("dateDebut", "2026-09-01")
                                .param("dateFin", "2026-09-30")
                )
                .andExpect(status().isNotFound());
    }
}