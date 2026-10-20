# Règles de calcul — Pinal

Ce document décrit les règles appliquées par l'API. Il fait référence pour les écrans Tableau de bord,
Production, Nutrition, Santé et Finances. Toute évolution d'une règle doit être répercutée ici et dans
les tests cités.

## Principes communs

- **Unité du lait : le litre (L)**, de la saisie aux indicateurs financiers. Aucune conversion kg → L.
- **Jour civil : UTC**, qui est aussi le fuseau de Dakar. Une traite appartient au jour civil UTC de sa date.
- **Périmètre : l'exploitation active** (`X-Exploitation-ID`). Aucune donnée d'une autre exploitation,
  même du même tenant, n'entre dans un calcul.
- **Donnée manquante ≠ zéro.** Une valeur qui dépend d'une donnée absente vaut `null` et un avertissement
  explique la cause. Un zéro n'est affiché que lorsqu'il est réellement nul (par exemple aucune traite).
- **Arrondi** : montants en FCFA et volumes à 2 décimales, arrondi commercial (`HALF_UP`).

## Saisie du lait

- Une traite appartient à la lactation en cours de l'animal. Aucune lactation n'est créée implicitement :
  elle démarre au vêlage ou par déclaration explicite (femelle, animal actif de l'exploitation).
- Date ni future (tolérance 10 min) ni antérieure au début de la lactation ; volume ≥ 0.
- Une seule traite `MATIN` et une seule `SOIR` par lactation et par jour (le type `AUTRE` n'est pas limité),
  garanti aussi par l'index `uk_traite_lactation_type_jour`.
- Saisie par lot : chaque ligne suit les mêmes règles ; les lignes refusées sont renvoyées avec leur motif.

## Prix

- Prix du lait et prix des aliments sont **propres à chaque exploitation** (le catalogue d'aliments aussi).
- Un prix s'applique sur `[dateDebut, dateFin]` (bornes incluses, `dateFin` vide = sans fin).
- **Succession** : un nouveau prix commençant après le début du prix en cours clôt ce dernier la veille.
  Tout autre recouvrement est refusé (contrainte d'exclusion SQL).

## Coût alimentaire

- Une ration compte un jour J si elle est `ACTIVE` ou `TERMINEE`, que `dateDebut ≤ J` et que `dateFin`
  est vide ou `≥ J`. Chaque ligne vaut `quantité × prix de l'aliment applicable le jour J`.
- Un jour sans prix pour un aliment distribué rend le coût **incomplet** : `coutAlimentation = null`,
  `coutAlimentationConnu` donne la part valorisable, et l'aliment est listé avec la période concernée.
- Les charges de catégorie `ALIMENTATION` (achats d'aliments) ne sont pas ajoutées au coût : la consommation
  est déjà valorisée par les rations, il n'y a pas de double comptage.
- Tests : `NutritionCostIntegrationTest`, `FeedCostPeriodEquivalenceIntegrationTest`,
  `NutritionHistoricalCostIntegrationTest`.

## Résultat financier d'une période `[début, fin]`

| Indicateur | Règle | Non calculable (`null`) si |
|---|---|---|
| Volume (L) | Σ traites de la période | — |
| Chiffre d'affaires | Σ (litres du jour × prix du lait applicable ce jour) | un jour de production n'a pas de prix |
| Coût alimentaire | voir ci-dessus | un aliment distribué n'a pas de prix |
| Autres charges | Σ charges saisies hors catégorie `ALIMENTATION` | — |
| Coût total | coût alimentaire + autres charges | coût alimentaire inconnu |
| Coût de revient / L | coût total ÷ volume | coût total inconnu ou volume nul |
| Prix moyen / L | CA ÷ volume | CA inconnu ou volume nul |
| Marge | CA − coût total | CA ou coût total inconnu |
| Taux de marge (%) | marge ÷ CA × 100 | marge inconnue ou CA nul |

Avertissements : `AUCUNE_TRAITE`, `PRIX_LAIT_MANQUANT`, `PRIX_ALIMENT_MANQUANT`.
Tests : `PeriodeFinanciereServiceTest`, `MilkAndFinanceIntegrationTest`, `BusinessRealityGateIT`.

- **Écran Finances** : mois en cours (du 1er à aujourd'hui) et historique des trois mois précédents,
  calculés en direct avec les mêmes règles.
- **Tableau de bord** : 30 derniers jours (aujourd'hui inclus) avec les mêmes règles, et prix du lait applicable aujourd'hui.
- **Snapshot de rentabilité** : enregistré seulement si toutes les données de la période sont connues.
- **Rentabilité d'un animal** : lait de l'animal × prix du jour − coût de ses rations. Résultat absent si
  un jour de la période n'a pas de ration ; erreur explicite si un jour produit n'a pas de prix du lait.

## Tableau de bord et production

- Production « aujourd'hui », « 7 jours », « 30 jours » : jours civils, aujourd'hui inclus.
- Moyenne par vache : litres des 7 derniers jours ÷ 7 ÷ vaches en lactation (`null` sans vache en lactation).
- Écran Production : évolution sur 7 jours par rapport aux 7 jours précédents, et pour chaque animal
  aujourd'hui par rapport à hier (`null` sans référence).

## Nutrition (jour courant)

- Coût journalier : rations effectives aujourd'hui × prix applicables aujourd'hui. `null` si une ligne
  n'a pas de prix ; le coût connu et la liste des aliments sans prix sont fournis.
- Ration moyenne : quantité des aliments exprimés en kg ÷ nombre de rations effectives.
- Aucun stock ni aucune livraison n'est géré : seule la consommation prévue par les rations est affichée.

## Santé

- Vaccination : événement dont la description commence par « Vaccination » (saisie du formulaire) ;
  sa date de fin est la date de rappel.
- Signalement ouvert : autre événement sans date de fin, sur un animal actif. Aucune sévérité n'est
  enregistrée : les signalements sont présentés comme « OUVERT ».
- Part des animaux sans signalement : animaux actifs sans signalement ouvert ÷ animaux actifs (`null` sans animal).
