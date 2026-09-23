package com.dairy.apipinal.identity.infrastructure.persistence;

import com.dairy.apipinal.identity.domain.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findFirstByTelephoneAndUsedFalseOrderByCreatedAtDesc(String telephone);
}
