package com.dairy.apipinal.nutrition.infrastructure.persistence;

import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.domain.StatutRation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RationRepository
        extends JpaRepository<Ration, UUID> {

    Optional<Ration> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );

    Page<Ration> findByTenantIdAndAnimalId(
            UUID tenantId,
            UUID animalId,
            Pageable pageable
    );

    boolean existsByIdAndTenantId(
            UUID id,
            UUID tenantId
    );

    @Query("""
    select count(r) > 0
    from Ration r
    where r.tenantId = :tenantId
      and r.animalId = :animalId
      and r.statut = :statut
      and r.dateDebut <= :dateFin
      and (
            r.dateFin is null
            or r.dateFin >= :dateDebut
          )
    """)
    boolean existsOverlappingActiveRationWithEndDate(
            @Param("tenantId") UUID tenantId,
            @Param("animalId") UUID animalId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin,
            @Param("statut") StatutRation statut
    );

    @Query("""
    select count(r) > 0
    from Ration r
    where r.tenantId = :tenantId
      and r.animalId = :animalId
      and r.statut = :statut
      and (
            r.dateFin is null
            or r.dateFin >= :dateDebut
          )
    """)
    boolean existsOverlappingActiveRationWithoutEndDate(
            @Param("tenantId") UUID tenantId,
            @Param("animalId") UUID animalId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("statut") StatutRation statut
    );

    @Query("""
        select r
        from Ration r
        where r.tenantId = :tenantId
          and r.animalId = :animalId
          and r.statut = com.dairy.apipinal.nutrition.domain.StatutRation.ACTIVE
          and r.dateDebut <= :date
          and (r.dateFin is null or r.dateFin >= :date)
        """)
    Optional<Ration> findActiveRationAtDate(
            UUID tenantId,
            UUID animalId,
            LocalDate date
    );

    @Query("""
    select r
    from Ration r
    where r.tenantId = :tenantId
      and r.animalId = :animalId
      and r.statut = :statut
      and r.dateDebut <= :date
      and (
            r.dateFin is null
            or r.dateFin >= :date
          )
    """)
    Optional<Ration> findActiveRationAtDate(
            @Param("tenantId") UUID tenantId,
            @Param("animalId") UUID animalId,
            @Param("date") LocalDate date,
            @Param("statut") StatutRation statut
    );

    @Query("""
    select r
    from Ration r
    where r.tenantId = :tenantId
      and r.statut = :statut
      and r.dateDebut <= :date
      and (
            r.dateFin is null
            or r.dateFin >= :date
          )
    """)
    List<Ration> findAllActiveRationsAtDate(
            @Param("tenantId") UUID tenantId,
            @Param("date") LocalDate date,
            @Param("statut") StatutRation statut
    );
}