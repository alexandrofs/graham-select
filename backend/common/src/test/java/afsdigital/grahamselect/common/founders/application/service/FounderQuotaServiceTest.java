package afsdigital.grahamselect.common.founders.application.service;

import afsdigital.grahamselect.common.founders.application.port.FounderQuotaRepositoryPort;
import afsdigital.grahamselect.common.founders.domain.model.FounderSlotsStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FounderQuotaServiceTest {

    @Mock
    private FounderQuotaRepositoryPort repositoryPort;

    private FounderQuotaService service;

    @BeforeEach
    void setUp() {
        service = new FounderQuotaService(repositoryPort, 90);
    }

    @Test
    void shouldReturnSlotsStatusFromRepositoryPort() {
        FounderSlotsStatus expected = new FounderSlotsStatus(250, 42, true);
        when(repositoryPort.getStatus()).thenReturn(expected);

        FounderSlotsStatus actual = service.getSlotsStatus();

        assertThat(actual).isEqualTo(expected);
        verify(repositoryPort).getStatus();
    }

    @Test
    void shouldTryClaimFounderSlotUsingUtcTimestamp() {
        when(repositoryPort.tryClaimFounderSlot(any(LocalDateTime.class))).thenReturn(true);

        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        boolean claimed = service.tryClaimFounderSlot();
        LocalDateTime after = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1);

        assertThat(claimed).isTrue();
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repositoryPort).tryClaimFounderSlot(captor.capture());
        assertThat(captor.getValue()).isAfter(before).isBefore(after);
    }

    @Test
    void shouldCalculateProTrialExpirationAddingConfiguredDays() {
        LocalDateTime nowUtc = LocalDateTime.of(2026, 11, 24, 12, 0, 0);

        LocalDateTime expiration = service.calculateProTrialExpiration(nowUtc);

        assertThat(expiration).isEqualTo(LocalDateTime.of(2027, 2, 22, 12, 0, 0));
    }
}
