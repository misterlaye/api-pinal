package com.dairy.apipinal.finance;

import com.dairy.apipinal.finance.application.CreatePrixVenteLait;
import com.dairy.apipinal.finance.application.PrixVenteLaitService;
import com.dairy.apipinal.finance.domain.PrixVenteLait;
import com.dairy.apipinal.finance.infrastructure.persistence.PrixVenteLaitRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrixVenteLaitServiceTest {

    @Mock
    private PrixVenteLaitRepository repository;

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private PrixVenteLaitService service;

    @Test
    void shouldCreateMilkSalePriceWithClosedPeriod() {

        UUID tenantId = UUID.randomUUID();

        LocalDate debut = LocalDate.of(2026, 9, 1);
        LocalDate fin = LocalDate.of(2026, 9, 30);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(repository.existsOverlappingPeriodWithEndDate(
                tenantId,
                debut,
                fin
        )).thenReturn(false);

        PrixVenteLait saved =
                new PrixVenteLait(
                        tenantId,
                        new BigDecimal("600.00"),
                        debut,
                        fin
                );

        when(repository.save(any(PrixVenteLait.class)))
                .thenReturn(saved);

        PrixVenteLait result =
                service.create(
                        new CreatePrixVenteLait(
                                new BigDecimal("600.00"),
                                debut,
                                fin
                        )
                );

        assertEquals(
                new BigDecimal("600.00"),
                result.getPrixParLitre()
        );

        verify(repository).save(any(PrixVenteLait.class));
    }

    @Test
    void shouldRejectOverlappingClosedPeriod() {

        UUID tenantId = UUID.randomUUID();

        LocalDate debut = LocalDate.of(2026, 9, 15);
        LocalDate fin = LocalDate.of(2026, 9, 30);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(repository.existsOverlappingPeriodWithEndDate(
                tenantId,
                debut,
                fin
        )).thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> service.create(
                        new CreatePrixVenteLait(
                                new BigDecimal("600.00"),
                                debut,
                                fin
                        )
                )
        );

        verify(repository, never())
                .save(any(PrixVenteLait.class));
    }

    @Test
    void shouldRejectOverlappingOpenEndedPeriod() {

        UUID tenantId = UUID.randomUUID();
        LocalDate debut = LocalDate.of(2026, 9, 15);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(repository.existsOverlappingOpenEndedPeriod(
                tenantId,
                debut
        )).thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> service.create(
                        new CreatePrixVenteLait(
                                new BigDecimal("600.00"),
                                debut,
                                null
                        )
                )
        );

        verify(repository, never())
                .save(any(PrixVenteLait.class));
    }

    @Test
    void shouldRetrieveApplicablePrice() {

        UUID tenantId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 20);

        PrixVenteLait price =
                new PrixVenteLait(
                        tenantId,
                        new BigDecimal("600.00"),
                        LocalDate.of(2026, 9, 1),
                        null
                );

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(repository.findApplicablePrice(
                tenantId,
                date
        )).thenReturn(Optional.of(price));

        PrixVenteLait result =
                service.getApplicablePrice(date);

        assertEquals(
                new BigDecimal("600.00"),
                result.getPrixParLitre()
        );
    }

    @Test
    void shouldRejectInvalidPeriod() {

        UUID tenantId = UUID.randomUUID();

        LocalDate debut = LocalDate.of(2026, 9, 30);
        LocalDate fin = LocalDate.of(2026, 9, 1);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(
                        new CreatePrixVenteLait(
                                new BigDecimal("600.00"),
                                debut,
                                fin
                        )
                )
        );

        verifyNoInteractions(repository);
    }
}