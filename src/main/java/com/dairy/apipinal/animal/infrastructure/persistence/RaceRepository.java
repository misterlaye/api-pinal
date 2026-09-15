package com.dairy.apipinal.animal.infrastructure.persistence;

import com.dairy.apipinal.animal.domain.Race;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RaceRepository extends JpaRepository<Race, UUID> {

    Optional<Race> findByIdAndActifTrue(UUID id);

    boolean existsByCodeIgnoreCase(String code);
}
