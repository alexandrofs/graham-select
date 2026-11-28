package afsdigital.grahamselect.api.openfinance.infrastructure.spring;

import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectionPort;
import afsdigital.grahamselect.common.openfinance.application.usecase.SyncOpenFinanceConnectionUseCase;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnection;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenFinanceSyncScheduler {

    private static final int MAX_CONCURRENT_SYNCS = 4;

    private final OpenFinanceConnectionPort connectionPort;
    private final SyncOpenFinanceConnectionUseCase syncOpenFinanceConnectionUseCase;

    @Scheduled(cron = "${openfinance.sync.cron:0 0 6 * * *}", zone = "America/Sao_Paulo")
    @SchedulerLock(name = "OpenFinanceSyncScheduler_syncAllActiveConnections", lockAtLeastFor = "1m", lockAtMostFor = "30m")
    public void syncAllActiveConnections() {
        log.info("Disparando agendamento OpenFinanceSyncScheduler às {}", OffsetDateTime.now(ZoneOffset.UTC));
        try {
            List<OpenFinanceConnection> activeConnections = connectionPort.findActiveConnections();
            log.info("Encontradas {} conexões ativas para sincronização programada Open Finance", activeConnections.size());
            if (activeConnections.isEmpty()) {
                return;
            }

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            int poolSize = Math.min(MAX_CONCURRENT_SYNCS, activeConnections.size());

            try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
                List<CompletableFuture<Void>> futures = activeConnections.stream()
                        .map(conn -> CompletableFuture.runAsync(
                                () -> syncSingleConnection(conn, successCount, failureCount),
                                executor))
                        .toList();
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            }

            log.info("Agendamento OpenFinanceSyncScheduler concluído. Sucesso: {}, Falhas: {}, Total: {}",
                    successCount.get(), failureCount.get(), activeConnections.size());
        } catch (Exception e) {
            log.error("Erro crítico no agendador OpenFinanceSyncScheduler: {}", e.getMessage(), e);
        }
    }

    private void syncSingleConnection(
            OpenFinanceConnection conn,
            AtomicInteger successCount,
            AtomicInteger failureCount
    ) {
        try {
            log.info("Sincronizando conexão agendada: connectionId={}, userId={}, institutionId={}",
                    conn.getId(), conn.getUserId(), conn.getInstitutionId());
            syncOpenFinanceConnectionUseCase.execute(conn.getId(), null, OpenFinanceSyncType.SCHEDULED);
            successCount.incrementAndGet();
        } catch (Exception e) {
            failureCount.incrementAndGet();
            log.error("Erro ao sincronizar conexão Open Finance agendada {}: {}", conn.getId(), e.getMessage(), e);
        }
    }
}
