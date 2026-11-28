package afsdigital.grahamselect.api.openfinance.infrastructure.spring;

import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectionPort;
import afsdigital.grahamselect.common.openfinance.application.usecase.SyncOpenFinanceConnectionUseCase;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnection;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnectionStatus;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenFinanceSyncSchedulerTest {

    @Mock
    private OpenFinanceConnectionPort connectionPort;

    @Mock
    private SyncOpenFinanceConnectionUseCase syncOpenFinanceConnectionUseCase;

    @InjectMocks
    private OpenFinanceSyncScheduler scheduler;

    @Test
    @DisplayName("Should sync all active connections and isolate failures")
    void testSyncAllActiveConnections() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        OpenFinanceConnection conn1 = OpenFinanceConnection.builder()
                .id(id1)
                .userId("user-1")
                .institutionId("XP_INVESTIMENTOS")
                .status(OpenFinanceConnectionStatus.ACTIVE)
                .build();

        OpenFinanceConnection conn2 = OpenFinanceConnection.builder()
                .id(id2)
                .userId("user-2")
                .institutionId("BTG_PACTUAL")
                .status(OpenFinanceConnectionStatus.ACTIVE)
                .build();

        when(connectionPort.findActiveConnections()).thenReturn(List.of(conn1, conn2));
        // conn1 throws exception, conn2 succeeds
        when(syncOpenFinanceConnectionUseCase.execute(eq(id1), isNull(), eq(OpenFinanceSyncType.SCHEDULED)))
                .thenThrow(new RuntimeException("API timeout"));

        scheduler.syncAllActiveConnections();

        verify(syncOpenFinanceConnectionUseCase, times(1)).execute(eq(id1), isNull(), eq(OpenFinanceSyncType.SCHEDULED));
        verify(syncOpenFinanceConnectionUseCase, times(1)).execute(eq(id2), isNull(), eq(OpenFinanceSyncType.SCHEDULED));
    }

    @Test
    @DisplayName("Should process multiple active connections concurrently in worker pool")
    void testSyncAllActiveConnectionsConcurrently() {
        OpenFinanceConnection conn1 = OpenFinanceConnection.builder()
                .id(UUID.randomUUID()).userId("u1").institutionId("XP").status(OpenFinanceConnectionStatus.ACTIVE).build();
        OpenFinanceConnection conn2 = OpenFinanceConnection.builder()
                .id(UUID.randomUUID()).userId("u2").institutionId("BTG").status(OpenFinanceConnectionStatus.ACTIVE).build();
        OpenFinanceConnection conn3 = OpenFinanceConnection.builder()
                .id(UUID.randomUUID()).userId("u3").institutionId("INTER").status(OpenFinanceConnectionStatus.ACTIVE).build();

        when(connectionPort.findActiveConnections()).thenReturn(List.of(conn1, conn2, conn3));

        CountDownLatch entered = new CountDownLatch(2);
        Set<String> workerThreads = ConcurrentHashMap.newKeySet();

        doAnswer(invocation -> {
            workerThreads.add(Thread.currentThread().getName());
            entered.countDown();
            entered.await(2, TimeUnit.SECONDS);
            return null;
        }).when(syncOpenFinanceConnectionUseCase).execute(
                org.mockito.ArgumentMatchers.any(), isNull(), eq(OpenFinanceSyncType.SCHEDULED));

        scheduler.syncAllActiveConnections();

        assertThat(workerThreads.size()).isGreaterThanOrEqualTo(2);
        verify(syncOpenFinanceConnectionUseCase, times(1)).execute(eq(conn1.getId()), isNull(), eq(OpenFinanceSyncType.SCHEDULED));
        verify(syncOpenFinanceConnectionUseCase, times(1)).execute(eq(conn2.getId()), isNull(), eq(OpenFinanceSyncType.SCHEDULED));
        verify(syncOpenFinanceConnectionUseCase, times(1)).execute(eq(conn3.getId()), isNull(), eq(OpenFinanceSyncType.SCHEDULED));
    }
}
