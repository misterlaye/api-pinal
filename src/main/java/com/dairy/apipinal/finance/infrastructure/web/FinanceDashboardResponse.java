package com.dairy.apipinal.finance.infrastructure.web;

import com.dairy.apipinal.finance.api.ResultatFinancier;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tableau de bord financier de l'exploitation courante.
 *
 * @param moisEnCours      indicateurs du 1er du mois à aujourd'hui (valeurs {@code null} si non calculables,
 *                         avec avertissements explicatifs)
 * @param historique       chiffre d'affaires et coût total des trois mois précédents et du mois en cours,
 *                         calculés selon les mêmes règles ({@code null} = non calculable)
 * @param chargesRepartition répartition des coûts du mois en cours, en FCFA
 * @param transactions     charges saisies du mois en cours
 */
public record FinanceDashboardResponse(
        ResultatFinancier moisEnCours,
        Historique historique,
        ChargesRepartition chargesRepartition,
        List<TransactionResponse> transactions
) {
    public record Historique(
            List<String> labels,
            List<BigDecimal> chiffreAffaires,
            List<BigDecimal> coutTotal
    ) {}

    public record ChargesRepartition(
            List<String> labels,
            List<BigDecimal> montants,
            boolean coutAlimentationPartiel
    ) {}
}
