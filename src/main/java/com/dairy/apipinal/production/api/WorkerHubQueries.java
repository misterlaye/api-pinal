package com.dairy.apipinal.production.api;

import java.util.List;
import java.util.UUID;

public interface WorkerHubQueries {

    WorkerHomeData getWorkerHomeData(UUID tenantId, UUID exploitationId);

    List<WorkerAnimalData> getWorkerAnimals(UUID tenantId, UUID exploitationId);

    WorkerAnimalDetailData getWorkerAnimalDetail(UUID tenantId, UUID animalId);

    // DTOs
    record WorkerHomeData(
            int totalAnimaux,
            int animauxASurveiller,
            int traitesDuMatin,
            int totalLactationAnimaux,
            double volumeCollecteMatin,
            List<WorkerActivityData> recentActivities
    ) {}

    record WorkerActivityData(
            UUID id,
            String type,
            String animalName,
            String animalId,
            String time,
            String description
    ) {}

    record WorkerAnimalData(
            UUID id,
            String name,
            String identifiant,
            String race,
            String lastMilkingTime,
            String lastMilkingVolume,
            String status,
            boolean done,
            @com.fasterxml.jackson.annotation.JsonProperty("isLactating")
            boolean isLactating
    ) {}

    record WorkerAnimalDetailData(
            UUID id,
            String name,
            String identifiant,
            String race,
            String etat,
            String lot,
            String ration,
            String lastMilkingTime,
            String lastMilkingVolume,
            List<WorkerHistoryItem> recentMilkings,
            @com.fasterxml.jackson.annotation.JsonProperty("isLactating")
            boolean isLactating
    ) {}

    record WorkerHistoryItem(
            UUID id,
            String date,
            String time,
            String volume
    ) {}
}
