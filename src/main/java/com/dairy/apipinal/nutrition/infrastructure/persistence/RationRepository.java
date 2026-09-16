package com.dairy.apipinal.nutrition.infrastructure.persistence;

import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.domain.StatutRation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface RationRepository
        extends JpaRepository<Ration, UUID> {

    Optional<Ration> findByIdAndTenantId(
            UUID id,
            UUID tenantId
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
}