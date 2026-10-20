package com.dairy.apipinal.dashboard.api;

import java.math.BigDecimal;

public record DashboardSummary(
        ProductionSummary production,
        HerdSummary troupeau,
        FinancialSummary finance
) {
    public record ProductionSummary(
            BigDecimal litresAujourdhui,
            BigDecimal litresDerniers7Jours,
            BigDecimal litresDerniers30Jours,
            BigDecimal moyenneParVacheLactationLitres
    ) {}

    public record HerdSummary(
            long totalAnimaux,
            long vachesEnLactation,
            long vachesTaries
    ) {}

    /**
     * @param prixLaitActuelParLitre prix de vente du lait applicable aujourd'hui ({@code null} si non configuré)
     * @param derniers30Jours        résultat financier des 30 derniers jours (règles du module Finance)
     */
    public record FinancialSummary(
            BigDecimal prixLaitActuelParLitre,
            com.dairy.apipinal.finance.api.ResultatFinancier derniers30Jours
    ) {}
}
