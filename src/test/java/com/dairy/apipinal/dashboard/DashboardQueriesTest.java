package com.dairy.apipinal.dashboard;

import com.dairy.apipinal.dashboard.api.DashboardSummary;
import com.dairy.apipinal.dashboard.application.DashboardQueriesImpl;
import com.dairy.apipinal.nutrition.api.ExploitationFeedCostReference;
import com.dairy.apipinal.nutrition.api.NutritionQueries;
import com.dairy.apipinal.production.api.ProductionQueries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardQueriesTest {

    @Mock
    private ProductionQueries productionQueries;

    @Mock
    private NutritionQueries nutritionQueries;

    private DashboardQueriesImpl dashboardQueries;

    @BeforeEach
    void setUp() {
        dashboardQueries = new DashboardQueriesImpl(productionQueries, nutritionQueries);
    }

    @Test
    void shouldReturnDashboardSummary() {
        when(productionQueries.getTotalMilkProductionKg(any(), any()))
                .thenReturn(new BigDecimal("120.50"))
                .thenReturn(new BigDecimal("840.00"))
                .thenReturn(new BigDecimal("3600.00"));

        ExploitationFeedCostReference feedCostRef = new ExploitationFeedCostReference(
                java.time.LocalDate.now().minusDays(30),
                java.time.LocalDate.now(),
                new BigDecimal("600000.00")
        );

        when(nutritionQueries.calculateTotalFeedCost(any(), any()))
                .thenReturn(Optional.of(feedCostRef));

        DashboardSummary summary = dashboardQueries.getDashboardSummary();

        assertNotNull(summary);
        assertEquals(new BigDecimal("120.50"), summary.production().kgAujourdhui());
        assertEquals(new BigDecimal("840.00"), summary.production().kgDerniers7Jours());
        assertEquals(new BigDecimal("3600.00"), summary.production().kgDerniers30Jours());
        assertEquals(12L, summary.troupeau().totalAnimaux());
        assertNotNull(summary.finance().chiffreAffairesEstimeLait30Jours());
    }
}
