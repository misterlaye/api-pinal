package com.dairy.apipinal.production.infrastructure.persistence;

import com.dairy.apipinal.production.domain.Traite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface TraiteRepository extends JpaRepository<Traite, UUID> {

    /** Doublon : même lactation, même type de traite, même jour civil [debutJour, finJour[. */
    boolean existsByLactationIdAndTypeAndDateHeureGreaterThanEqualAndDateHeureLessThan(
            UUID lactationId,
            com.dairy.apipinal.production.domain.TypeTraite type,
            OffsetDateTime debutJour,
            OffsetDateTime finJour
    );

    List<Traite> findAllByLactationIdAndTenantIdAndExploitationIdOrderByDateHeureAsc(
            UUID lactationId,
            UUID tenantId,
            UUID exploitationId
    );

    @Query("""
        select coalesce(sum(t.quantiteLitres), 0)
        from Traite t
        where t.lactationId = :lactationId
          and t.tenantId = :tenantId
          and t.dateHeure >= :dateDebut
          and t.dateHeure < :dateFinExclusive
        """)
    BigDecimal sumQuantiteLitresByLactationAndPeriod(
            @Param("lactationId") UUID lactationId,
            @Param("tenantId") UUID tenantId,
            @Param("dateDebut") OffsetDateTime dateDebut,
            @Param("dateFinExclusive") OffsetDateTime dateFinExclusive
    );

    @Query("""
        select coalesce(sum(t.quantiteLitres), 0)
        from Traite t
        where t.tenantId = :tenantId
          and t.exploitationId = :exploitationId
          and t.dateHeure >= :dateDebut
          and t.dateHeure < :dateFinExclusive
        """)
    BigDecimal sumQuantiteLitresByExploitationAndPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("exploitationId") UUID exploitationId,
            @Param("dateDebut") OffsetDateTime dateDebut,
            @Param("dateFinExclusive") OffsetDateTime dateFinExclusive
    );

    /**
     * Volume de lait par jour civil (UTC, fuseau de l'Afrique de l'Ouest) sur [debut, finExclusive[.
     * Une seule requête groupée : [jour (LocalDate), litres (BigDecimal)].
     */
    @Query(value = """
        select cast(t.date_heure at time zone 'UTC' as date) as jour, sum(t.quantite_litres) as litres
        from traite t
        where t.tenant_id = :tenantId
          and t.exploitation_id = :exploitationId
          and t.date_heure >= :debut
          and t.date_heure < :finExclusive
        group by cast(t.date_heure at time zone 'UTC' as date)
        """, nativeQuery = true)
    List<Object[]> sumLitresParJour(
            @Param("tenantId") UUID tenantId,
            @Param("exploitationId") UUID exploitationId,
            @Param("debut") OffsetDateTime debut,
            @Param("finExclusive") OffsetDateTime finExclusive
    );
}
