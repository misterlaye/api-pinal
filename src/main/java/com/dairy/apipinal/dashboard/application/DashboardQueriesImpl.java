package com.dairy.apipinal.dashboard.application;

import com.dairy.apipinal.dashboard.api.DashboardQueries;
import com.dairy.apipinal.dashboard.api.DashboardSummary;
import com.dairy.apipinal.finance.api.FinanceQueries;
import com.dairy.apipinal.finance.api.ResultatFinancier;
import com.dairy.apipinal.production.api.ProductionQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tableau de bord propriétaire.
 * <ul>
 *     <li>Production en litres par jours civils (UTC) : aujourd'hui, 7 et 30 derniers jours, aujourd'hui inclus ;</li>
 *     <li>moyenne par vache = litres des 7 derniers jours / 7 / vaches actuellement en lactation ;</li>
 *     <li>finance : résultat des 30 derniers jours calculé par le module Finance, mêmes règles que l'écran Finances.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class DashboardQueriesImpl implements DashboardQueries {

    private final ProductionQueries productionQueries;
    private final FinanceQueries financeQueries;
    private final JdbcTemplate jdbcTemplate;
    private final TenantContext tenantContext;

    public DashboardQueriesImpl(
            ProductionQueries productionQueries,
            FinanceQueries financeQueries,
            JdbcTemplate jdbcTemplate,
            TenantContext tenantContext
    ) {
        this.productionQueries = productionQueries;
        this.financeQueries = financeQueries;
        this.jdbcTemplate = jdbcTemplate;
        this.tenantContext = tenantContext;
    }

    @Override
    public DashboardSummary getDashboardSummary() {
        LocalDate today = LocalDate.now();
        UUID tenantId = tenantContext.currentTenantId();
        UUID exploitationId = tenantContext.currentExploitationId();

        // Une seule requête groupée pour les 30 derniers jours, découpée ensuite en mémoire.
        Map<LocalDate, BigDecimal> laitParJour = productionQueries.getDailyMilkProductionLitres(today.minusDays(29), today);
        BigDecimal litresAujourdhui = somme(laitParJour, today, today);
        BigDecimal litres7Jours = somme(laitParJour, today.minusDays(6), today);
        BigDecimal litres30Jours = somme(laitParJour, today.minusDays(29), today);

        Long totalAnimals = jdbcTemplate.queryForObject(
                "SELECT COUNT(id) FROM animal WHERE statut = 'ACTIF' AND tenant_id = ? AND exploitation_id = ?",
                Long.class,
                tenantId,
                exploitationId
        );

        Long vachesEnLactation = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT l.animal_id) FROM lactation l JOIN animal a ON l.animal_id = a.id WHERE a.statut = 'ACTIF' AND l.statut = 'EN_COURS' AND l.tenant_id = ? AND l.exploitation_id = ?",
                Long.class,
                tenantId,
                exploitationId
        );

        // Vaches taries : femelles actives qui ont déjà eu au moins une lactation, mais n'en ont pas en cours
        Long vachesTaries = jdbcTemplate.queryForObject(
                "SELECT COUNT(id) FROM animal a WHERE statut = 'ACTIF' AND sexe = 'FEMELLE' AND tenant_id = ? AND exploitation_id = ? " +
                "AND EXISTS (SELECT 1 FROM lactation l WHERE l.animal_id = a.id) " +
                "AND NOT EXISTS (SELECT 1 FROM lactation l2 WHERE l2.animal_id = a.id AND l2.statut = 'EN_COURS')",
                Long.class,
                tenantId,
                exploitationId
        );

        long enLactation = vachesEnLactation != null ? vachesEnLactation : 0L;
        BigDecimal moyenneParVache = enLactation > 0
                ? litres7Jours.divide(BigDecimal.valueOf(7L * enLactation), 2, RoundingMode.HALF_UP)
                : null;

        List<BigDecimal> prix = jdbcTemplate.queryForList(
                "SELECT prix_par_litre FROM prix_vente_lait WHERE tenant_id = ? AND exploitation_id = ? AND date_debut <= CURRENT_DATE AND (date_fin IS NULL OR date_fin >= CURRENT_DATE) ORDER BY date_debut DESC LIMIT 1",
                BigDecimal.class,
                tenantId,
                exploitationId
        );

        ResultatFinancier derniers30Jours = financeQueries.calculerPeriode(today.minusDays(29), today);

        return new DashboardSummary(
                new DashboardSummary.ProductionSummary(
                        litresAujourdhui,
                        litres7Jours,
                        litres30Jours,
                        moyenneParVache
                ),
                new DashboardSummary.HerdSummary(
                        totalAnimals != null ? totalAnimals : 0L,
                        enLactation,
                        vachesTaries != null ? vachesTaries : 0L
                ),
                new DashboardSummary.FinancialSummary(
                        prix.isEmpty() ? null : prix.getFirst(),
                        derniers30Jours
                )
        );
    }

    private static BigDecimal somme(Map<LocalDate, BigDecimal> parJour, LocalDate debut, LocalDate fin) {
        return parJour.entrySet().stream()
                .filter(e -> !e.getKey().isBefore(debut) && !e.getKey().isAfter(fin))
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
