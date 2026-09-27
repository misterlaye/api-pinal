package com.dairy.apipinal.production.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnalyseLaitRepository extends JpaRepository<AnalyseLait, UUID> {
    List<AnalyseLait> findAllByLactationIdOrderByDateAnalyseDesc(UUID lactationId);
}
