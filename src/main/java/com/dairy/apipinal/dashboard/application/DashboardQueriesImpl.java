package com.dairy.apipinal.dashboard.application;

import com.dairy.apipinal.dashboard.api.DashboardQueries;
import com.dairy.apipinal.dashboard.api.DashboardSummary;
import com.dairy.apipinal.nutrition.api.ExploitationFeedCostReference;
import com.dairy.apipinal.nutrition.api.NutritionQueries;
import com.dairy.apipinal.production.api.ProductionQueries;
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

    public DashboardQueriesImpl(
            ProductionQueries productionQueries,
            NutritionQueries nutritionQueries
    ) {
        this.productionQueries = productionQueries;
        this.nutritionQueries = nutritionQueries;
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

        DashboardSummary.HerdSummary troupeau = new DashboardSummary.HerdSummary(
                12L, 8L, 4L
        );

        Optional<ExploitationFeedCostReference> feedCost30J = nutritionQueries.calculateTotalFeedCost(
                LocalDate.now().minusDays(30),
                LocalDate.now()
        );

        BigDecimal totalFeedCost30J = feedCost30J.map(ExploitationFeedCostReference::coutAlimentation).orElse(BigDecimal.ZERO);
        BigDecimal prixUnitaireLait = new BigDecimal("450.00");
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
