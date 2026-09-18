package com.dairy.apipinal.nutrition.infrastructure.persistence;

import com.dairy.apipinal.nutrition.domain.Aliment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AlimentRepository extends JpaRepository<Aliment, UUID> {

    boolean existsByCode(String code);

    Optional<Aliment> findByCode(String code);

    Optional<Aliment> findByIdAndActifTrue(UUID id);
}