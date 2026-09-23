package com.dairy.apipinal.identity.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "otp_code")
public class OtpCode {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20)
    private String telephone;

    @Column(name = "code_hash", nullable = false, length = 256)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected OtpCode() {
    }

    public OtpCode(String telephone, String codeHash, OffsetDateTime expiresAt) {
        this.id = UUID.randomUUID();
        this.telephone = telephone;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.attempts = 0;
        this.used = false;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public boolean isUsed() {
        return used;
    }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markUsed() {
        this.used = true;
    }
}
