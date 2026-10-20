package com.dairy.apipinal.nutrition.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Tableau de bord nutrition de l'exploitation courante, pour aujourd'hui.
 * <p>
 * Règles (identiques au calcul du coût alimentaire de la Finance) : une ration est prise en compte si elle
 * est ACTIVE ou TERMINEE et effective aujourd'hui ; chaque ligne est valorisée au prix de l'aliment applicable aujourd'hui.
 * Une ligne sans prix n'est jamais valorisée à zéro : le coût concerné vaut {@code null} et l'aliment est listé
 * dans {@code alimentsSansPrix}. Aucun stock n'est géré : seule la consommation prévue par les rations est affichée.
 */
public record NutritionDashboardSummary(
        Kpis kpis,
        List<RationGroup> rationGroups,
        List<ConsommationAliment> consommations,
        List<AlimentSansPrix> alimentsSansPrix
) {
    /**
     * @param coutJournalier        coût alimentaire du jour, {@code null} si au moins une ligne n'a pas de prix
     * @param coutJournalierConnu   somme des lignes valorisables (égale à coutJournalier si complet)
     * @param coutJournalierComplet vrai si toutes les lignes ont un prix
     * @param rationMoyenneKg       quantité moyenne d'aliments exprimés en kg par vache sous ration, {@code null} sans ration
     * @param alimentsAuCatalogue   nombre d'aliments actifs du catalogue de l'exploitation
     * @param couverture            part des animaux actifs ayant une ration effective (%), {@code null} sans animal
     */
    public record Kpis(
            BigDecimal coutJournalier,
            BigDecimal coutJournalierConnu,
            boolean coutJournalierComplet,
            BigDecimal rationMoyenneKg,
            long alimentsAuCatalogue,
            BigDecimal couverture,
            String couvertureLabel
    ) {}

    /**
     * @param coutParJour      coût de la ration pour la journée, {@code null} si une ligne n'a pas de prix
     * @param alimentsSansPrix noms des aliments de la ration sans prix applicable aujourd'hui
     */
    public record RationGroup(
            UUID id,
            String name,
            String composition,
            BigDecimal coutParJour,
            List<String> alimentsSansPrix
    ) {}

    /**
     * Consommation journalière prévue par les rations effectives (pas un stock).
     */
    public record ConsommationAliment(
            UUID id,
            String name,
            String unit,
            BigDecimal consommationJournaliere,
            BigDecimal prixUnitaire
    ) {}

    public record AlimentSansPrix(
            UUID id,
            String name
    ) {}
}
