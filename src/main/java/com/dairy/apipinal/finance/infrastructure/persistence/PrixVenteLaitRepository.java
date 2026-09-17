package com.dairy.apipinal.finance.infrastructure.persistence;

import com.dairy.apipinal.finance.domain.PrixVenteLait;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrixVenteLaitRepository
        extends JpaRepository<PrixVenteLait, UUID> {

    @Query("""
        select p
        from PrixVenteLait p
        where p.tenantId = :tenantId
          and p.dateDebut <= :date
          and (
                p.dateFin is null
                or p.dateFin >= :date
              )
        """)
    Optional<PrixVenteLait> findApplicablePrice(
            @Param("tenantId") UUID tenantId,
            @Param("date") LocalDate date
    );

    List<PrixVenteLait> findAllByTenantIdOrderByDateDebutDesc(
            UUID tenantId
    );

    @Query("""
        select count(p) > 0
        from PrixVenteLait p
        where p.tenantId = :tenantId
          and p.dateDebut <= :dateFin
          and (
                p.dateFin is null
                or p.dateFin >= :dateDebut
              )
        """)
    boolean existsOverlappingPeriodWithEndDate(
            @Param("tenantId") UUID tenantId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    @Query("""
        select count(p) > 0
        from PrixVenteLait p
        where p.tenantId = :tenantId
          and (
                p.dateFin is null
                or p.dateFin >= :dateDebut
              )
        """)
    boolean existsOverlappingOpenEndedPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("dateDebut") LocalDate dateDebut
    );
}