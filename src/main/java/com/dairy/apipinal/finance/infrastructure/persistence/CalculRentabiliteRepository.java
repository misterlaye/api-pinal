package com.dairy.apipinal.finance.infrastructure.persistence;

import com.dairy.apipinal.finance.domain.CalculRentabilite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CalculRentabiliteRepository
        extends JpaRepository<CalculRentabilite, UUID> {

}