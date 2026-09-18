package com.dairy.apipinal.identity.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "membership",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_membership_user_exploitation",
                columnNames = {"utilisateur_id", "exploitation_id"}
        )
)
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "utilisateur_id", nullable = false)
    private UUID utilisateurId;

    @Column(name = "exploitation_id", nullable = false)
    private UUID exploitationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoleExploitation role;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Membership() {
    }

    public Membership(
            UUID utilisateurId,
            UUID exploitationId,
            RoleExploitation role
    ) {
        this.utilisateurId = utilisateurId;
        this.exploitationId = exploitationId;
        this.role = role;

        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUtilisateurId() {
        return utilisateurId;
    }

    public UUID getExploitationId() {
        return exploitationId;
    }

    public RoleExploitation getRole() {
        return role;
    }
}
