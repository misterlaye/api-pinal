package com.dairy.apipinal.health.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tableau de bord santé de l'exploitation courante.
 * <p>
 * Convention : un événement est une vaccination lorsque sa description commence par « Vaccination »
 * (valeur saisie par le formulaire) ; sa date de fin porte alors la date de rappel. Les autres événements
 * sans date de fin sont des signalements ouverts. Aucune sévérité n'est enregistrée : les signalements
 * sont présentés comme « OUVERT », sans gravité inventée.
 */
public record HealthDashboardSummary(
        Kpis kpis,
        List<Alert> alerts,
        List<HistoryEvent> history,
        List<Vaccination> vaccinations
) {
    public record Kpis(
            int alertsCount,
            String alertsSubtitle,
            String vaccinationsRatio,
            String vaccinationsSubtitle,
            String nextVisit,
            String nextVisitSubtitle,
            Integer healthScore,
            String healthScoreSubtitle
    ) {}

    public record Alert(
            UUID id,
            UUID animalId,
            String animalName,
            String animalIdentifier,
            String type,
            String severity,
            String date,
            String message
    ) {}

    public record HistoryEvent(
            UUID id,
            UUID animalId,
            String animalName,
            String date,
            String type,
            String description,
            String status
    ) {}

    public record Vaccination(
            UUID id,
            String name,
            String vaccine,
            String avatar,
            String date,
            boolean isUrgent
    ) {}
}
