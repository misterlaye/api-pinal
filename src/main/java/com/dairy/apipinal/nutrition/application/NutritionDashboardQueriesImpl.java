package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.nutrition.api.NutritionDashboardQueries;
import com.dairy.apipinal.nutrition.api.NutritionDashboardSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class NutritionDashboardQueriesImpl implements NutritionDashboardQueries {

    private final JdbcTemplate jdbcTemplate;

    public NutritionDashboardQueriesImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public NutritionDashboardSummary getDashboardSummary(UUID tenantId, UUID exploitationId) {
        // 1. Total active animals in the exploitation/tenant
        Long totalAnimals;
        if (exploitationId != null) {
            totalAnimals = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM animal WHERE tenant_id = ? AND exploitation_id = ? AND statut = 'ACTIF'",
                    Long.class, tenantId, exploitationId
            );
        } else {
            totalAnimals = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM animal WHERE tenant_id = ? AND statut = 'ACTIF'",
                    Long.class, tenantId
            );
        }
        if (totalAnimals == null) totalAnimals = 0L;

        // 2. Fetch active rations
        String activeRationsSql = """
            SELECT r.id as ration_id, r.animal_id, a.identifiant, a.nom as animal_nom
            FROM ration r
            JOIN animal a ON r.animal_id = a.id
            WHERE r.tenant_id = ? AND r.statut = 'ACTIVE'
            """ + (exploitationId != null ? " AND a.exploitation_id = ?" : "") + " ORDER BY a.identifiant ASC";

        List<Map<String, Object>> activeRationRows = (exploitationId != null)
                ? jdbcTemplate.queryForList(activeRationsSql, tenantId, exploitationId)
                : jdbcTemplate.queryForList(activeRationsSql, tenantId);

        double totalDailyCost = 0.0;
        double totalDailyKg = 0.0;
        List<NutritionDashboardSummary.RationGroup> rationGroups = new ArrayList<>();

        for (Map<String, Object> rRow : activeRationRows) {
            UUID rationId = (UUID) rRow.get("ration_id");
            String animalIdentifiant = (String) rRow.get("identifiant");
            String animalNom = (String) rRow.get("animal_nom");

            // Query lines for this ration with current price
            String linesSql = """
                SELECT lr.quantite, al.nom as aliment_nom, al.unite,
                       (SELECT p.prix_unitaire
                        FROM prix_aliment p
                        WHERE p.aliment_id = lr.aliment_id
                          AND p.date_debut <= CURRENT_DATE
                          AND (p.date_fin IS NULL OR p.date_fin >= CURRENT_DATE)
                        ORDER BY p.date_debut DESC LIMIT 1) as prix_unitaire
                FROM ligne_ration lr
                JOIN aliment al ON lr.aliment_id = al.id
                WHERE lr.ration_id = ?
            """;

            List<Map<String, Object>> lines = jdbcTemplate.queryForList(linesSql, rationId);
            double rationCost = 0.0;
            List<String> compParts = new ArrayList<>();

            for (Map<String, Object> l : lines) {
                BigDecimal qty = (BigDecimal) l.get("quantite");
                String alimentNom = (String) l.get("aliment_nom");
                String unite = (String) l.get("unite");
                BigDecimal pu = (BigDecimal) l.get("prix_unitaire");

                double q = (qty != null) ? qty.doubleValue() : 0.0;
                totalDailyKg += q;

                if (pu != null) {
                    rationCost += q * pu.doubleValue();
                }

                compParts.add(alimentNom + " (" + q + " " + (unite != null ? unite.toLowerCase() : "") + ")");
            }

            totalDailyCost += rationCost;
            String composition = compParts.isEmpty() ? "Aucun aliment renseigné" : String.join(", ", compParts);

            rationGroups.add(new NutritionDashboardSummary.RationGroup(
                    rationId,
                    animalNom + " (" + animalIdentifiant + ")",
                    composition,
                    Math.round(rationCost * 100.0) / 100.0,
                    1,
                    "ACTIVE"
            ));
        }

        int activeRationsCount = activeRationRows.size();
        double averageRation = (activeRationsCount > 0) ? (totalDailyKg / activeRationsCount) : 0.0;

        // 3. Aliments in catalogue count
        Long alimentsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM aliment WHERE actif = true", Long.class
        );
        if (alimentsCount == null) alimentsCount = 0L;

        // 4. Stocks / Aliments consumption summary
        String alimentConsoSql = """
            SELECT al.id, al.nom, al.unite,
                   COALESCE(SUM(CASE WHEN r.statut = 'ACTIVE' THEN lr.quantite ELSE 0 END), 0) as conso_jour,
                   (SELECT p.prix_unitaire
                    FROM prix_aliment p
                    WHERE p.aliment_id = al.id
                      AND p.date_debut <= CURRENT_DATE
                      AND (p.date_fin IS NULL OR p.date_fin >= CURRENT_DATE)
                    ORDER BY p.date_debut DESC LIMIT 1) as prix_unitaire
            FROM aliment al
            LEFT JOIN ligne_ration lr ON al.id = lr.aliment_id
            LEFT JOIN ration r ON lr.ration_id = r.id AND r.tenant_id = ? AND r.statut = 'ACTIVE'
            WHERE al.actif = true
            GROUP BY al.id, al.nom, al.unite
            ORDER BY conso_jour DESC, al.nom ASC
        """;

        List<Map<String, Object>> stockRows = jdbcTemplate.queryForList(alimentConsoSql, tenantId);
        List<NutritionDashboardSummary.Stock> stocks = new ArrayList<>();

        for (Map<String, Object> s : stockRows) {
            UUID alId = (UUID) s.get("id");
            String alNom = (String) s.get("nom");
            String alUnite = (String) s.get("unite");
            BigDecimal conso = (BigDecimal) s.get("conso_jour");
            BigDecimal pu = (BigDecimal) s.get("prix_unitaire");

            double dailyCons = (conso != null) ? conso.doubleValue() : 0.0;
            String status = (pu != null) ? (pu.setScale(0, RoundingMode.HALF_UP) + " FCFA/" + (alUnite != null ? alUnite.toLowerCase() : "")) : "Prix à définir";

            stocks.add(new NutritionDashboardSummary.Stock(
                    alId,
                    alNom,
                    0.0,
                    alUnite != null ? alUnite.toLowerCase() : "",
                    status,
                    dailyCons
            ));
        }

        double coveragePct = (totalAnimals > 0) ? ((double) activeRationsCount / totalAnimals) * 100.0 : 0.0;
        String coverageLabel = activeRationsCount + " / " + totalAnimals + " vaches couvertes";

        NutritionDashboardSummary.Kpis kpis = new NutritionDashboardSummary.Kpis(
                Math.round(totalDailyCost),
                0.0,
                Math.round(averageRation * 10.0) / 10.0,
                "kg / vache sous ration",
                alimentsCount.doubleValue(),
                "Aliments au catalogue",
                Math.round(coveragePct * 10.0) / 10.0,
                coverageLabel
        );

        return new NutritionDashboardSummary(kpis, rationGroups, stocks, Collections.emptyList());
    }
}
