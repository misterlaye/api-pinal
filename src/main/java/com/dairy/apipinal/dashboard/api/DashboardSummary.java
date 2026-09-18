package com.dairy.apipinal.dashboard.api;

import java.math.BigDecimal;

public record DashboardSummary(
        ProductionSummary production,
        HerdSummary troupeau,
        FinancialSummary finance
) {
    public record ProductionSummary(
            BigDecimal kgAujourdhui,
            BigDecimal kgDerniers7Jours,
            BigDecimal kgDerniers30Jours,
            BigDecimal moyenneParVacheLactationKg
    ) {}

    public record HerdSummary(
            long totalAnimaux,
            long vachesEnLactation,
            long vachesTaries
    ) {}

    public record FinancialSummary(
            BigDecimal prixMoyenLaitParKg,
            BigDecimal coutMoyenRationParJour,
            BigDecimal chiffreAffairesEstimeLait30Jours,
            BigDecimal margeEstimeeSurCoutAlimentaire30Jours
    ) {}
}
