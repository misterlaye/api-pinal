package com.dairy.apipinal.finance.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Résultat financier d'une période [debut, fin] (bornes incluses) de l'exploitation courante.
 * <p>
 * Règles (voir REGLES-CALCUL.md) :
 * <ul>
 *     <li>volume : somme des traites, en litres ;</li>
 *     <li>chiffre d'affaires : volume de chaque jour × prix de vente du lait applicable ce jour-là ;</li>
 *     <li>coût alimentaire : rations effectives valorisées au prix de chaque aliment applicable chaque jour ;</li>
 *     <li>autres charges : charges saisies hors catégorie ALIMENTATION (les achats d'aliments ne sont pas
 *     comptés une seconde fois, la consommation étant valorisée par les rations) ;</li>
 *     <li>une valeur qui dépend d'une donnée manquante vaut {@code null} (jamais zéro) et un avertissement
 *     explique la cause.</li>
 * </ul>
 * Montants en FCFA arrondis à 2 décimales (arrondi commercial), volumes en litres à 2 décimales.
 */
public record ResultatFinancier(
        LocalDate debut,
        LocalDate fin,
        BigDecimal volumeLitres,
        BigDecimal chiffreAffaires,
        BigDecimal prixMoyenParLitre,
        BigDecimal coutAlimentation,
        BigDecimal coutAlimentationConnu,
        BigDecimal autresCharges,
        BigDecimal coutTotal,
        BigDecimal coutRevientParLitre,
        BigDecimal marge,
        BigDecimal tauxMarge,
        List<Avertissement> avertissements
) {
    public record Avertissement(String code, String message) {
    }

    public boolean complet() {
        return chiffreAffaires != null && coutTotal != null;
    }
}
