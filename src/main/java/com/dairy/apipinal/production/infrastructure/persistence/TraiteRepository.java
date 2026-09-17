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

    List<Traite> findAllByLactationIdAndTenantIdOrderByDateHeureAsc(
            UUID lactationId,
            UUID tenantId
    );

    @Query("""
        select coalesce(sum(t.quantiteKg), 0)
        from Traite t
        where t.lactationId = :lactationId
          and t.tenantId = :tenantId
          and t.dateHeure >= :dateDebut
          and t.dateHeure < :dateFinExclusive
        """)
    BigDecimal sumQuantiteKgByLactationAndPeriod(
            @Param("lactationId") UUID lactationId,
            @Param("tenantId") UUID tenantId,
            @Param("dateDebut") OffsetDateTime dateDebut,
            @Param("dateFinExclusive") OffsetDateTime dateFinExclusive
    );

    @Query("""
        select coalesce(sum(t.quantiteKg), 0)
        from Traite t
        where t.tenantId = :tenantId
          and t.dateHeure >= :dateDebut
          and t.dateHeure < :dateFinExclusive
        """)
    BigDecimal sumQuantiteKgByTenantAndPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("dateDebut") OffsetDateTime dateDebut,
            @Param("dateFinExclusive") OffsetDateTime dateFinExclusive
    );
}