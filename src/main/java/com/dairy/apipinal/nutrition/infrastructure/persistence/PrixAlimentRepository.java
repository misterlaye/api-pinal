package com.dairy.apipinal.nutrition.infrastructure.persistence;

import com.dairy.apipinal.nutrition.domain.PrixAliment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
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

    @Query("""
        select count(p) > 0
        from PrixAliment p
        where p.alimentId = :alimentId
          and p.dateDebut <= :dateFin
          and (p.dateFin is null or p.dateFin >= :dateDebut)
        """)
    boolean existsOverlappingPeriodWithEndDate(
            UUID alimentId,
            LocalDate dateDebut,
            LocalDate dateFin
    );

    @Query("""
        select count(p) > 0
        from PrixAliment p
        where p.alimentId = :alimentId
          and (p.dateFin is null or p.dateFin >= :dateDebut)
        """)
    boolean existsOverlappingOpenEndedPeriod(
            UUID alimentId,
            LocalDate dateDebut
    );

    List<PrixAliment> findAllByAlimentIdOrderByDateDebutDesc(
            UUID alimentId
    );
}