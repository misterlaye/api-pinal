package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.nutrition.api.NutritionDashboardQueries;
import com.dairy.apipinal.nutrition.api.NutritionDashboardSummary;
import com.dairy.apipinal.nutrition.api.NutritionDashboardSummary.AlimentSansPrix;
import com.dairy.apipinal.nutrition.api.NutritionDashboardSummary.ConsommationAliment;
import com.dairy.apipinal.nutrition.api.NutritionDashboardSummary.RationGroup;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tableau de bord nutrition du jour : trois requêtes, indépendantes du nombre de rations
 * (animaux actifs, lignes des rations effectives avec prix applicable, catalogue avec prix applicable).
 */
@Service
@Transactional(readOnly = true)
public class NutritionDashboardQueriesImpl implements NutritionDashboardQueries {

    private static final int SCALE = 2;

    private final JdbcTemplate jdbcTemplate;

    public NutritionDashboardQueriesImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public NutritionDashboardSummary getDashboardSummary(UUID tenantId, UUID exploitationId) {
        LocalDate today = LocalDate.now();

        Long totalAnimals = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM animal WHERE tenant_id = ? AND exploitation_id = ? AND statut = 'ACTIF'",
                Long.class, tenantId, exploitationId
        );

        // Lignes des rations ACTIVES effectives aujourd'hui, avec le prix de l'aliment applicable aujourd'hui.
        List<Ligne> lignes = jdbcTemplate.query("""
                SELECT r.id AS ration_id, a.nom AS animal_nom, a.identifiant,
                       al.id AS aliment_id, al.nom AS aliment_nom, al.unite, lr.quantite,
                       (SELECT p.prix_unitaire FROM prix_aliment p
                         WHERE p.aliment_id = al.id
                           AND p.date_debut <= ?
                           AND (p.date_fin IS NULL OR p.date_fin >= ?)) AS prix_unitaire
                FROM ration r
                JOIN animal a ON a.id = r.animal_id
                LEFT JOIN ligne_ration lr ON lr.ration_id = r.id
                LEFT JOIN aliment al ON al.id = lr.aliment_id
                WHERE r.tenant_id = ? AND r.exploitation_id = ? AND r.statut IN ('ACTIVE', 'TERMINEE')
                  AND r.date_debut <= ? AND (r.date_fin IS NULL OR r.date_fin >= ?)
                ORDER BY a.identifiant, al.nom
                """, this::ligne, today, today, tenantId, exploitationId, today, today);

        Map<UUID, List<Ligne>> parRation = new LinkedHashMap<>();
        for (Ligne ligne : lignes) {
            parRation.computeIfAbsent(ligne.rationId(), k -> new ArrayList<>()).add(ligne);
        }

        BigDecimal coutConnu = BigDecimal.ZERO;
        BigDecimal kgTotal = BigDecimal.ZERO;
        Map<UUID, String> sansPrix = new LinkedHashMap<>();
        Map<UUID, BigDecimal> consommationParAliment = new LinkedHashMap<>();
        List<RationGroup> rationGroups = new ArrayList<>();

        for (List<Ligne> ration : parRation.values()) {
            Ligne premiere = ration.getFirst();
            BigDecimal coutRation = BigDecimal.ZERO;
            Set<String> manquants = new LinkedHashSet<>();
            List<String> composition = new ArrayList<>();
            for (Ligne ligne : ration) {
                if (ligne.alimentId() == null) {
                    continue; // ration sans ligne
                }
                composition.add(ligne.alimentNom() + " (" + ligne.quantite().stripTrailingZeros().toPlainString()
                        + " " + ligne.unite().toLowerCase() + ")");
                consommationParAliment.merge(ligne.alimentId(), ligne.quantite(), BigDecimal::add);
                if ("KG".equals(ligne.unite())) {
                    kgTotal = kgTotal.add(ligne.quantite());
                }
                if (ligne.prixUnitaire() == null) {
                    manquants.add(ligne.alimentNom());
                    sansPrix.put(ligne.alimentId(), ligne.alimentNom());
                } else {
                    BigDecimal cout = ligne.quantite().multiply(ligne.prixUnitaire());
                    coutRation = coutRation.add(cout);
                    coutConnu = coutConnu.add(cout);
                }
            }
            rationGroups.add(new RationGroup(
                    premiere.rationId(),
                    premiere.animalNom() + " (" + premiere.identifiant() + ")",
                    composition.isEmpty() ? "Aucun aliment renseigné" : String.join(", ", composition),
                    manquants.isEmpty() ? scale(coutRation) : null,
                    List.copyOf(manquants)
            ));
        }

        // Catalogue de l'exploitation avec prix applicable aujourd'hui, et consommation prévue par les rations.
        List<ConsommationAliment> consommations = jdbcTemplate.query("""
                SELECT al.id, al.nom, al.unite,
                       (SELECT p.prix_unitaire FROM prix_aliment p
                         WHERE p.aliment_id = al.id
                           AND p.date_debut <= ?
                           AND (p.date_fin IS NULL OR p.date_fin >= ?)) AS prix_unitaire
                FROM aliment al
                WHERE al.actif = true AND al.tenant_id = ? AND al.exploitation_id = ?
                ORDER BY al.nom
                """, (rs, i) -> {
            UUID id = rs.getObject("id", UUID.class);
            return new ConsommationAliment(
                    id,
                    rs.getString("nom"),
                    rs.getString("unite").toLowerCase(),
                    scale(consommationParAliment.getOrDefault(id, BigDecimal.ZERO)),
                    rs.getBigDecimal("prix_unitaire")
            );
        }, today, today, tenantId, exploitationId);
        consommations = consommations.stream()
                .sorted((a, b) -> b.consommationJournaliere().compareTo(a.consommationJournaliere()))
                .toList();

        long animaux = totalAnimals != null ? totalAnimals : 0L;
        int nbRations = parRation.size();
        boolean complet = sansPrix.isEmpty();

        NutritionDashboardSummary.Kpis kpis = new NutritionDashboardSummary.Kpis(
                complet ? scale(coutConnu) : null,
                scale(coutConnu),
                complet,
                nbRations > 0 ? kgTotal.divide(BigDecimal.valueOf(nbRations), 1, RoundingMode.HALF_UP) : null,
                consommations.size(),
                animaux > 0
                        ? BigDecimal.valueOf(nbRations * 100L).divide(BigDecimal.valueOf(animaux), 1, RoundingMode.HALF_UP)
                        : null,
                nbRations + " / " + animaux + " animaux sous ration"
        );

        return new NutritionDashboardSummary(
                kpis,
                rationGroups,
                consommations,
                sansPrix.entrySet().stream().map(e -> new AlimentSansPrix(e.getKey(), e.getValue())).toList()
        );
    }

    private Ligne ligne(ResultSet rs, int rowNum) throws SQLException {
        return new Ligne(
                rs.getObject("ration_id", UUID.class),
                rs.getString("animal_nom"),
                rs.getString("identifiant"),
                rs.getObject("aliment_id", UUID.class),
                rs.getString("aliment_nom"),
                rs.getString("unite"),
                rs.getBigDecimal("quantite"),
                rs.getBigDecimal("prix_unitaire")
        );
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private record Ligne(
            UUID rationId,
            String animalNom,
            String identifiant,
            UUID alimentId,
            String alimentNom,
            String unite,
            BigDecimal quantite,
            BigDecimal prixUnitaire
    ) {}
}
