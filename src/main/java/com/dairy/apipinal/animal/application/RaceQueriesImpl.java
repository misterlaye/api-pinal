package com.dairy.apipinal.animal.application;

import com.dairy.apipinal.animal.api.RaceQueries;
import com.dairy.apipinal.animal.infrastructure.persistence.RaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RaceQueriesImpl implements RaceQueries {

    private final RaceRepository raceRepository;

    public RaceQueriesImpl(RaceRepository raceRepository) {
        this.raceRepository = raceRepository;
    }

    @Override
    public List<RaceInfo> getActiveRaces() {
        return raceRepository.findAllByActifTrueOrderByLibelleAsc()
                .stream()
                .map(r -> new RaceInfo(r.getId(), r.getCode(), r.getLibelle()))
                .collect(Collectors.toList());
    }
}
