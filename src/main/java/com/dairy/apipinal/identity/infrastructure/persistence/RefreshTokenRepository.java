package com.dairy.apipinal.identity.infrastructure.persistence;

import com.dairy.apipinal.identity.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    void deleteByUserIdAndRevokedTrue(UUID userId);
}
