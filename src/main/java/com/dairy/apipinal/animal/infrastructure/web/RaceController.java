package com.dairy.apipinal.animal.infrastructure.web;

import com.dairy.apipinal.animal.api.RaceQueries;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/races")
@org.springframework.security.access.prepost.PreAuthorize("@authz.isWorkerOrOwner()")
public class RaceController {

    private final RaceQueries raceQueries;

    public RaceController(RaceQueries raceQueries) {
        this.raceQueries = raceQueries;
    }

    @GetMapping
    public List<RaceQueries.RaceInfo> getActiveRaces() {
        return raceQueries.getActiveRaces();
    }
}
