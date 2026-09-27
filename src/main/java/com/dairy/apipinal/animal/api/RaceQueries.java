package com.dairy.apipinal.animal.api;

import java.util.List;
import java.util.UUID;

public interface RaceQueries {
    List<RaceInfo> getActiveRaces();

    record RaceInfo(UUID id, String code, String libelle) {}
}
