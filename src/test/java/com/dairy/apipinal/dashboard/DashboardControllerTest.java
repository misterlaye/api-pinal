package com.dairy.apipinal.dashboard;

import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import com.dairy.apipinal.anomaly.api.AnomalyQueries;
import com.dairy.apipinal.dashboard.api.DashboardQueries;
import com.dairy.apipinal.dashboard.api.DashboardSummary;
import com.dairy.apipinal.dashboard.infrastructure.web.DashboardController;
import com.dairy.apipinal.shared.infrastructure.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(GlobalExceptionHandler.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardQueries dashboardQueries;

    @MockitoBean
    private AnomalyQueries anomalyQueries;

    @Test
    @WithMockUser
    void shouldGetDashboardSummary() throws Exception {
        DashboardSummary summary = new DashboardSummary(
                new DashboardSummary.ProductionSummary(new BigDecimal("100"), new BigDecimal("700"), new BigDecimal("3000"), new BigDecimal("100")),
                new DashboardSummary.HerdSummary(10, 7, 3),
                new DashboardSummary.FinancialSummary(new BigDecimal("450"), new BigDecimal("2000"), new BigDecimal("1350000"), new BigDecimal("750000"))
        );

        when(dashboardQueries.getDashboardSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.production.kgAujourdhui").value(100));
    }

    @Test
    @WithMockUser
    void shouldGetDashboardAnomalies() throws Exception {
        AnomalyAlert alert = new AnomalyAlert(
                UUID.randomUUID().toString(),
                "DROP_MILK_PRODUCTION",
                "AVERTISSEMENT",
                "Chute de production",
                "Description",
                null,
                OffsetDateTime.now()
        );

        when(anomalyQueries.detectAnomalies()).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/v1/dashboard/anomalies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("DROP_MILK_PRODUCTION"));
    }
}
