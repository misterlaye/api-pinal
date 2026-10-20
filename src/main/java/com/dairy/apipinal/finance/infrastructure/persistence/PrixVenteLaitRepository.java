package com.dairy.apipinal.finance.infrastructure.persistence;

import com.dairy.apipinal.finance.domain.PrixVenteLait;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Prix de vente du lait, propres à chaque exploitation.
 */
public interface PrixVenteLaitRepository
        extends JpaRepository<PrixVenteLait, UUID> {

    @Query("""
        select p
        from PrixVenteLait p
        where p.tenantId = :tenantId
          and p.exploitationId = :exploitationId
          and p.dateDebut <= :date
          and (
                p.dateFin is null
                or p.dateFin >= :date
              )
        """)
    Optional<PrixVenteLait> findApplicablePrice(
            @Param("tenantId") UUID tenantId,
            @Param("exploitationId") UUID exploitationId,
            @Param("date") LocalDate date
    );

    /** Prix dont la période recoupe [dateDebut, dateFin] : chargés en une seule requête pour un calcul de période. */
    @Query("""
        select p
        from PrixVenteLait p
        where p.tenantId = :tenantId
          and p.exploitationId = :exploitationId
          and p.dateDebut <= :dateFin
          and (p.dateFin is null or p.dateFin >= :dateDebut)
        """)
    List<PrixVenteLait> findAllOverlappingPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("exploitationId") UUID exploitationId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    List<PrixVenteLait> findAllByTenantIdAndExploitationIdOrderByDateDebutDesc(
            UUID tenantId,
            UUID exploitationId
    );

    Optional<PrixVenteLait> findFirstByTenantIdAndExploitationIdAndDateFinIsNull(
            UUID tenantId,
            UUID exploitationId
    );

    @Query("""
        select count(p) > 0
        from PrixVenteLait p
        where p.tenantId = :tenantId
          and p.exploitationId = :exploitationId
          and p.dateDebut <= :dateFin
          and (
                p.dateFin is null
                or p.dateFin >= :dateDebut
              )
        """)
    boolean existsOverlappingPeriodWithEndDate(
            @Param("tenantId") UUID tenantId,
            @Param("exploitationId") UUID exploitationId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin
    );

    @Query("""
        select count(p) > 0
        from PrixVenteLait p
        where p.tenantId = :tenantId
          and p.exploitationId = :exploitationId
          and (
                p.dateFin is null
                or p.dateFin >= :dateDebut
              )
        """)
    boolean existsOverlappingOpenEndedPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("exploitationId") UUID exploitationId,
            @Param("dateDebut") LocalDate dateDebut
    );
}
