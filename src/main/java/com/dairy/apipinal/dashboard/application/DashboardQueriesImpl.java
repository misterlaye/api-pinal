package com.dairy.apipinal.dashboard.application;

import com.dairy.apipinal.dashboard.api.DashboardQueries;
import com.dairy.apipinal.dashboard.api.DashboardSummary;
import com.dairy.apipinal.nutrition.api.ExploitationFeedCostReference;
import com.dairy.apipinal.nutrition.api.NutritionQueries;
import com.dairy.apipinal.production.api.ProductionQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DashboardQueriesImpl implements DashboardQueries {

    private final ProductionQueries productionQueries;
    private final NutritionQueries nutritionQueries;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final TenantContext tenantContext;

    public DashboardQueriesImpl(
            ProductionQueries productionQueries,
            NutritionQueries nutritionQueries,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            TenantContext tenantContext
    ) {
        this.productionQueries = productionQueries;
        this.nutritionQueries = nutritionQueries;
        this.jdbcTemplate = jdbcTemplate;
        this.tenantContext = tenantContext;
    }

    @Override
    public DashboardSummary getDashboardSummary() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime todayStart = now.minusDays(1);
        OffsetDateTime last7DaysStart = now.minusDays(7);
        OffsetDateTime last30DaysStart = now.minusDays(30);

        BigDecimal todayProd = productionQueries.getTotalMilkProductionKg(todayStart, now);
        BigDecimal last7DaysProd = productionQueries.getTotalMilkProductionKg(last7DaysStart, now);
        BigDecimal last30DaysProd = productionQueries.getTotalMilkProductionKg(last30DaysStart, now);

        if (todayProd == null) todayProd = BigDecimal.ZERO;
        if (last7DaysProd == null) last7DaysProd = BigDecimal.ZERO;
        if (last30DaysProd == null) last30DaysProd = BigDecimal.ZERO;

        BigDecimal avgMoyenneVache = last7DaysProd.divide(new BigDecimal("7"), 2, RoundingMode.HALF_UP);

        DashboardSummary.ProductionSummary production = new DashboardSummary.ProductionSummary(
                todayProd,
                last7DaysProd,
                last30DaysProd,
                avgMoyenneVache
        );

        java.util.UUID tenantId = tenantContext.currentTenantId();
        java.util.UUID exploitationId = tenantContext.currentExploitationId();

        Long totalAnimals = jdbcTemplate.queryForObject(
                "SELECT COUNT(id) FROM animal WHERE statut = 'ACTIF' AND tenant_id = ? AND exploitation_id = ?", 
                Long.class, 
                tenantId, 
                exploitationId
        );
        if (totalAnimals == null) totalAnimals = 0L;

        Long vachesLaitieres = jdbcTemplate.queryForObject(
                "SELECT COUNT(id) FROM animal WHERE statut = 'ACTIF' AND sexe = 'FEMELLE' AND tenant_id = ? AND exploitation_id = ?", 
                Long.class, 
                tenantId, 
                exploitationId
        );
        if (vachesLaitieres == null) vachesLaitieres = 0L;
        
        Long vachesTaries = 0L; // Simplified

        DashboardSummary.HerdSummary troupeau = new DashboardSummary.HerdSummary(
                totalAnimals, vachesLaitieres, vachesTaries
        );

        Optional<ExploitationFeedCostReference> feedCost30J = nutritionQueries.calculateTotalFeedCost(
                LocalDate.now().minusDays(30),
                LocalDate.now()
        );

        BigDecimal totalFeedCost30J = feedCost30J.map(ExploitationFeedCostReference::coutAlimentation).orElse(BigDecimal.ZERO);
        BigDecimal prixUnitaireLait;
        try {
            prixUnitaireLait = jdbcTemplate.queryForObject(
                    "SELECT prix_par_litre FROM prix_vente_lait WHERE tenant_id = ? AND date_debut <= CURRENT_DATE AND (date_fin IS NULL OR date_fin >= CURRENT_DATE) ORDER BY date_debut DESC LIMIT 1",
                    BigDecimal.class,
                    tenantId
            );
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            prixUnitaireLait = new BigDecimal("450.00");
        }

        BigDecimal caEstime30J = last30DaysProd.multiply(prixUnitaireLait);
        BigDecimal margeEstimee30J = caEstime30J.subtract(totalFeedCost30J);

        DashboardSummary.FinancialSummary finance = new DashboardSummary.FinancialSummary(
                prixUnitaireLait,
                totalFeedCost30J.divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP),
                caEstime30J,
                margeEstimee30J
        );

        return new DashboardSummary(production, troupeau, finance);
    }
}
