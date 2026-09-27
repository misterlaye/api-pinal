package com.dairy.apipinal.animal.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mouvement_sortie")
public class MouvementSortie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "animal_id", nullable = false)
    private UUID animalId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "exploitation_id", nullable = false)
    private UUID exploitationId;

    @Column(name = "date_sortie", nullable = false)
    private LocalDate dateSortie;

    @Column(nullable = false, length = 255)
    private String motif;

    @Column(name = "prix_vente")
    private BigDecimal prixVente;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    protected MouvementSortie() {
    }

    public MouvementSortie(
            UUID animalId,
            UUID tenantId,
            UUID exploitationId,
            LocalDate dateSortie,
            String motif,
            BigDecimal prixVente,
            UUID actorId
    ) {
        this.animalId = animalId;
        this.tenantId = tenantId;
        this.exploitationId = exploitationId;
        this.dateSortie = dateSortie;
        this.motif = motif;
        this.prixVente = prixVente;

        this.createdAt = OffsetDateTime.now();
        this.createdBy = actorId;
    }

    public UUID getId() { return id; }
    public UUID getAnimalId() { return animalId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getExploitationId() { return exploitationId; }
    public LocalDate getDateSortie() { return dateSortie; }
    public String getMotif() { return motif; }
    public BigDecimal getPrixVente() { return prixVente; }
}
