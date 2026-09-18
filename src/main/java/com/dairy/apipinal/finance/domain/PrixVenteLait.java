package com.dairy.apipinal.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "prix_vente_lait")
public class PrixVenteLait {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "exploitation_id")
    private UUID exploitationId;

    @Column(name = "prix_par_litre", nullable = false, precision = 19, scale = 4)
    private BigDecimal prixParLitre;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Version
    @Column(nullable = false)
    private long version;

    protected PrixVenteLait() {
    }

    public PrixVenteLait(
            UUID tenantId,
            BigDecimal prixParLitre,
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        this.tenantId = Objects.requireNonNull(
                tenantId,
                "Le tenant est obligatoire."
        );

        this.prixParLitre = requirePositivePrice(prixParLitre);

        this.dateDebut = Objects.requireNonNull(
                dateDebut,
                "La date de début est obligatoire."
        );

        if (dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException(
                    "La date de fin ne peut pas être antérieure à la date de début."
            );
        }

        this.dateFin = dateFin;
    }

    private static BigDecimal requirePositivePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Le prix du lait doit être strictement positif."
            );
        }

        return price;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getExploitationId() {
        return exploitationId;
    }

    public BigDecimal getPrixParLitre() {
        return prixParLitre;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }
}