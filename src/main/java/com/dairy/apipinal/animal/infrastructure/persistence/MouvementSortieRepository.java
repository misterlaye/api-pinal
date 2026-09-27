package com.dairy.apipinal.animal.infrastructure.persistence;

import com.dairy.apipinal.animal.domain.MouvementSortie;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface MouvementSortieRepository extends JpaRepository<MouvementSortie, UUID> {
    List<MouvementSortie> findByExploitationIdOrderByDateSortieDesc(UUID exploitationId);
    List<MouvementSortie> findByAnimalId(UUID animalId);
}
