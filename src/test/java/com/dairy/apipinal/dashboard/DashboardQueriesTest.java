package com.dairy.apipinal.dashboard;

import com.dairy.apipinal.dashboard.api.DashboardSummary;
import com.dairy.apipinal.dashboard.application.DashboardQueriesImpl;
import com.dairy.apipinal.finance.api.FinanceQueries;
import com.dairy.apipinal.finance.api.ResultatFinancier;
import com.dairy.apipinal.production.api.ProductionQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardQueriesTest {

    @Mock private ProductionQueries productionQueries;
    @Mock private FinanceQueries financeQueries;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private TenantContext tenantContext;

    private DashboardQueriesImpl dashboardQueries;
    private final UUID tenant = UUID.randomUUID();
    private final UUID exploitation = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        dashboardQueries = new DashboardQueriesImpl(productionQueries, financeQueries, jdbcTemplate, tenantContext);
        when(tenantContext.currentTenantId()).thenReturn(tenant);
        when(tenantContext.currentExploitationId()).thenReturn(exploitation);
    }

    @Test
    void productionUsesCalendarDaysAndFinanceComesFromTheFinanceModule() {
        LocalDate today = LocalDate.now();
        Map<LocalDate, BigDecimal> lait = new TreeMap<>();
        lait.put(today, new BigDecimal("40"));
        lait.put(today.minusDays(6), new BigDecimal("30"));
        lait.put(today.minusDays(7), new BigDecimal("100"));   // hors des 7 derniers jours
        lait.put(today.minusDays(29), new BigDecimal("10"));
        when(productionQueries.getDailyMilkProductionLitres(today.minusDays(29), today)).thenReturn(lait);

        when(jdbcTemplate.queryForObject(contains("FROM animal WHERE statut = 'ACTIF'"), eq(Long.class), eq(tenant), eq(exploitation)))
                .thenReturn(12L);
        when(jdbcTemplate.queryForObject(contains("FROM lactation l JOIN animal"), eq(Long.class), eq(tenant), eq(exploitation)))
                .thenReturn(2L);
        when(jdbcTemplate.queryForObject(contains("sexe = 'FEMELLE'"), eq(Long.class), eq(tenant), eq(exploitation)))
                .thenReturn(3L);
        when(jdbcTemplate.queryForList(anyString(), eq(BigDecimal.class), eq(tenant), eq(exploitation)))
                .thenReturn(List.of(new BigDecimal("450")));

        ResultatFinancier finance = new ResultatFinancier(today.minusDays(29), today, new BigDecimal("180"), null, null,
                null, null, BigDecimal.ZERO, null, null, null, null,
                List.of(new ResultatFinancier.Avertissement("PRIX_LAIT_MANQUANT", "Prix manquant")));
        when(financeQueries.calculerPeriode(today.minusDays(29), today)).thenReturn(finance);

        DashboardSummary summary = dashboardQueries.getDashboardSummary();

        assertThat(summary.production().litresAujourdhui()).isEqualByComparingTo("40");
        assertThat(summary.production().litresDerniers7Jours()).isEqualByComparingTo("70");
        assertThat(summary.production().litresDerniers30Jours()).isEqualByComparingTo("180");
        assertThat(summary.production().moyenneParVacheLactationLitres()).isEqualByComparingTo("5");   // 70 / 7 / 2
        assertThat(summary.troupeau().totalAnimaux()).isEqualTo(12);
        assertThat(summary.troupeau().vachesEnLactation()).isEqualTo(2);
        assertThat(summary.troupeau().vachesTaries()).isEqualTo(3);
        assertThat(summary.finance().prixLaitActuelParLitre()).isEqualByComparingTo("450");
        assertThat(summary.finance().derniers30Jours()).isSameAs(finance);
    }

    @Test
    void averagePerCowIsUnknownWithoutCowsInLactation() {
        LocalDate today = LocalDate.now();
        when(productionQueries.getDailyMilkProductionLitres(today.minusDays(29), today)).thenReturn(Map.of());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(tenant), eq(exploitation))).thenReturn(0L);
        when(jdbcTemplate.queryForList(anyString(), eq(BigDecimal.class), eq(tenant), eq(exploitation))).thenReturn(List.of());

        DashboardSummary summary = dashboardQueries.getDashboardSummary();

        assertThat(summary.production().litresAujourdhui()).isZero();
        assertThat(summary.production().moyenneParVacheLactationLitres()).isNull();
        assertThat(summary.finance().prixLaitActuelParLitre()).isNull();
    }
}
