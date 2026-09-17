package com.dairy.apipinal.nutrition.infrastructure.persistence;

import com.dairy.apipinal.nutrition.domain.PrixAliment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PrixAlimentRepository extends JpaRepository<PrixAliment, UUID> {

    @Query("""
            select p
            from PrixAliment p
            where p.alimentId = :alimentId
              and p.dateDebut <= :date
              and (p.dateFin is null or p.dateFin >= :date)
            """)
    Optional<PrixAliment> findApplicablePrice(
            UUID alimentId,
            LocalDate date
    );
}