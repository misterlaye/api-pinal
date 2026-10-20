package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.nutrition.domain.Aliment;
import com.dairy.apipinal.nutrition.infrastructure.persistence.AlimentRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AlimentService {

    private final AlimentRepository alimentRepository;

    private final TenantContext tenantContext;

    public AlimentService(AlimentRepository alimentRepository, TenantContext tenantContext) {
        this.alimentRepository = alimentRepository;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public Aliment create(CreateAliment command) {
        UUID tenantId = tenantContext.currentTenantId();
        UUID exploitationId = tenantContext.currentExploitationId();

        if (command.code() != null && alimentRepository.existsByExploitationIdAndCode(exploitationId, command.code().trim())) {
            throw new IllegalStateException("Un aliment avec ce code existe déjà dans votre catalogue.");
        }

        Aliment aliment = new Aliment(
                tenantId,
                exploitationId,
                command.code(),
                command.nom(),
                command.categorie(),
                command.unite()
        );

        return alimentRepository.save(aliment);
    }

    @Transactional(readOnly = true)
    public List<Aliment> getAll() {
        return alimentRepository.findAllByTenantIdAndExploitationIdOrderByNomAsc(
                tenantContext.currentTenantId(),
                tenantContext.currentExploitationId()
        );
    }

    @Transactional(readOnly = true)
    public Aliment getById(UUID id) {
        return alimentRepository.findByIdAndTenantIdAndExploitationIdAndActifTrue(
                        id,
                        tenantContext.currentTenantId(),
                        tenantContext.currentExploitationId()
                )
                .orElseThrow(() -> new IllegalArgumentException("Aliment introuvable"));
    }
}
