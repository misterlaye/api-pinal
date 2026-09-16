package com.dairy.apipinal.nutrition;

import com.dairy.apipinal.nutrition.api.NutritionQueries;
import com.dairy.apipinal.nutrition.api.RationReference;
import com.dairy.apipinal.nutrition.application.NutritionQueriesImpl;
import com.dairy.apipinal.nutrition.domain.OrigineRation;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.domain.StatutRation;
import com.dairy.apipinal.nutrition.infrastructure.persistence.RationRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NutritionQueriesTest {

    @Mock
    private RationRepository rationRepository;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private Ration ration;

    private NutritionQueries nutritionQueries;

    private UUID tenantId;
    private UUID animalId;
    private UUID rationId;

    @BeforeEach
    void setUp() {
        nutritionQueries = new NutritionQueriesImpl(
                rationRepository,
                tenantContext
        );

        tenantId = UUID.randomUUID();
        animalId = UUID.randomUUID();
        rationId = UUID.randomUUID();
    }

    @Test
    void shouldReturnRationReference() {

        LocalDate dateDebut = LocalDate.of(2026, 9, 1);
        LocalDate dateFin = LocalDate.of(2026, 9, 30);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(rationRepository.findByIdAndTenantId(
                rationId,
                tenantId
        )).thenReturn(Optional.of(ration));

        when(ration.getId())
                .thenReturn(rationId);

        when(ration.getTenantId())
                .thenReturn(tenantId);

        when(ration.getAnimalId())
                .thenReturn(animalId);

        when(ration.getDateDebut())
                .thenReturn(dateDebut);

        when(ration.getDateFin())
                .thenReturn(dateFin);

        when(ration.getStatut())
                .thenReturn(StatutRation.ACTIVE);

        when(ration.getOrigine())
                .thenReturn(OrigineRation.ACTUELLE);

        RationReference reference =
                nutritionQueries.getRation(rationId);

        assertThat(reference.id())
                .isEqualTo(rationId);

        assertThat(reference.tenantId())
                .isEqualTo(tenantId);

        assertThat(reference.animalId())
                .isEqualTo(animalId);

        assertThat(reference.dateDebut())
                .isEqualTo(dateDebut);

        assertThat(reference.dateFin())
                .isEqualTo(dateFin);

        assertThat(reference.statut())
                .isEqualTo(StatutRation.ACTIVE);

        assertThat(reference.origine())
                .isEqualTo(OrigineRation.ACTUELLE);

        verify(tenantContext)
                .currentTenantId();

        verify(rationRepository)
                .findByIdAndTenantId(
                        rationId,
                        tenantId
                );
    }

    @Test
    void shouldReturnFalseWhenRationDoesNotExist() {

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(rationRepository.existsByIdAndTenantId(
                rationId,
                tenantId
        )).thenReturn(false);

        assertThat(
                nutritionQueries.exists(rationId)
        ).isFalse();

        verify(tenantContext)
                .currentTenantId();

        verify(rationRepository)
                .existsByIdAndTenantId(
                        rationId,
                        tenantId
                );
    }
}