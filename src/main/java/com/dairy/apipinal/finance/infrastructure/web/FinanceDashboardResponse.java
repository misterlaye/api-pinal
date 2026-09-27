package com.dairy.apipinal.finance.infrastructure.web;

import java.math.BigDecimal;
import java.util.List;

public record FinanceDashboardResponse(
        Kpis kpis,
        RevenueVsCharges revenueVsCharges,
        ChargesRepartition chargesRepartition,
        Rentability rentability,
        List<TransactionResponse> transactions
) {
    public record Kpis(
            BigDecimal coutRevient,
            int coutRevientTrend,
            BigDecimal prixMoyenVente,
            BigDecimal margeBrute,
            int margeBruteTrend,
            BigDecimal chiffreAffaires,
            int chiffreAffairesTrend
    ) {}

    public record RevenueVsCharges(
            List<String> labels,
            List<BigDecimal> revenues,
            List<BigDecimal> charges
    ) {}

    public record ChargesRepartition(
            List<String> labels,
            List<Integer> data,
            List<String> colors
    ) {}

    public record Rentability(
            BigDecimal netProfit,
            BigDecimal netMarginPercentage,
            int trend
    ) {}
}
