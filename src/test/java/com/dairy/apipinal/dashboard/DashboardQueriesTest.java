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
import org.springframework.jdbc.core.JdbcTemplate;
import com.dairy.apipinal.shared.security.TenantContext;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardQueriesTest {

    @Mock
    private ProductionQueries productionQueries;

    @Mock
    private NutritionQueries nutritionQueries;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private TenantContext tenantContext;

    private DashboardQueriesImpl dashboardQueries;

    @BeforeEach
    void setUp() {
        dashboardQueries = new DashboardQueriesImpl(
                productionQueries,
                nutritionQueries,
                jdbcTemplate,
                tenantContext
        );
    }

    @Test
    void shouldReturnDashboardSummary() {

        // =========================================================
        // Context variables
        // =========================================================
        java.util.UUID testTenant = java.util.UUID.randomUUID();
        java.util.UUID testExploitation = java.util.UUID.randomUUID();

        // =========================================================
        // Production
        // =========================================================

        when(productionQueries.getTotalMilkProductionKg(any(), any()))
                .thenReturn(new BigDecimal("120.50"))
                .thenReturn(new BigDecimal("840.00"))
                .thenReturn(new BigDecimal("3600.00"));

        // =========================================================
        // Troupeau
        // =========================================================

        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(id) FROM animal WHERE statut = 'ACTIF' AND tenant_id = ? AND exploitation_id = ?"),
                eq(Long.class),
                eq(testTenant),
                eq(testExploitation)
        )).thenReturn(12L);

        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(id) FROM animal WHERE statut = 'ACTIF' AND sexe = 'FEMELLE' AND tenant_id = ? AND exploitation_id = ?"),
                eq(Long.class),
                eq(testTenant),
                eq(testExploitation)
        )).thenReturn(12L);

        // =========================================================
        // Nutrition
        // =========================================================

        ExploitationFeedCostReference feedCostRef =
                new ExploitationFeedCostReference(
                        java.time.LocalDate.now().minusDays(30),
                        java.time.LocalDate.now(),
                        new BigDecimal("600000.00")
                );

        when(nutritionQueries.calculateTotalFeedCost(any(), any()))
                .thenReturn(Optional.of(feedCostRef));

        // =========================================================
        // Context
        // =========================================================
        
        when(tenantContext.currentTenantId()).thenReturn(testTenant);
        when(tenantContext.currentExploitationId()).thenReturn(testExploitation);

        // =========================================================
        // Exécution
        // =========================================================

        DashboardSummary summary = dashboardQueries.getDashboardSummary();

        // =========================================================
        // Vérifications
        // =========================================================

        assertNotNull(summary);

        // Production
        assertEquals(
                new BigDecimal("120.50"),
                summary.production().kgAujourdhui()
        );

        assertEquals(
                new BigDecimal("840.00"),
                summary.production().kgDerniers7Jours()
        );

        assertEquals(
                new BigDecimal("3600.00"),
                summary.production().kgDerniers30Jours()
        );

        assertEquals(
                new BigDecimal("120.00"),
                summary.production().moyenneParVacheLactationKg()
        );

        // Troupeau
        assertEquals(
                12L,
                summary.troupeau().totalAnimaux()
        );

        assertEquals(
                12L,
                summary.troupeau().vachesEnLactation()
        );

        assertEquals(
                0L,
                summary.troupeau().vachesTaries()
        );

        // Finance
        assertEquals(
                new BigDecimal("450.00"),
                summary.finance().prixMoyenLaitParKg()
        );

        assertEquals(
                new BigDecimal("20000.00"),
                summary.finance().coutMoyenRationParJour()
        );

        assertEquals(
                new BigDecimal("1620000.0000"),
                summary.finance().chiffreAffairesEstimeLait30Jours()
        );

        assertEquals(
                new BigDecimal("1020000.0000"),
                summary.finance().margeEstimeeSurCoutAlimentaire30Jours()
        );
    }
}