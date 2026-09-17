package com.dairy.apipinal.production.infrastructure.persistence;

import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.production.domain.StatutLactation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LactationRepository extends JpaRepository<Lactation, UUID> {

    Optional<Lactation> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );

    boolean existsByAnimalIdAndTenantIdAndStatut(
            UUID animalId,
            UUID tenantId,
            StatutLactation statut
    );

    Optional<Lactation> findByAnimalIdAndTenantIdAndStatut(
            UUID animalId,
            UUID tenantId,
            StatutLactation statut
    );

    @Query("""
    select l
    from Lactation l
    where l.animalId = :animalId
      and l.tenantId = :tenantId
      and l.dateDebut <= :dateFin
      and (
            l.dateFin is null
            or l.dateFin >= :dateDebut
          )
    order by l.dateDebut asc
    """)
    List<Lactation> findAllOverlappingPeriod(
            @Param("animalId") UUID animalId,
            @Param("tenantId") UUID tenantId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );
}