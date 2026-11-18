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

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenFinanceSyncScheduler {

    private final OpenFinanceConnectionPort connectionPort;
    private final SyncOpenFinanceConnectionUseCase syncOpenFinanceConnectionUseCase;

    @Scheduled(cron = "${openfinance.sync.cron:0 0 6 * * *}", zone = "America/Sao_Paulo")
    @SchedulerLock(name = "OpenFinanceSyncScheduler_syncAllActiveConnections", lockAtLeastFor = "1m", lockAtMostFor = "30m")
    public void syncAllActiveConnections() {
        log.info("Disparando agendamento OpenFinanceSyncScheduler às {}", OffsetDateTime.now(ZoneOffset.UTC));
        try {
            List<OpenFinanceConnection> activeConnections = connectionPort.findActiveConnections();
            log.info("Encontradas {} conexões ativas para sincronização programada Open Finance", activeConnections.size());

            int successCount = 0;
            int failureCount = 0;

            for (OpenFinanceConnection conn : activeConnections) {
                try {
                    log.info("Sincronizando conexão agendada: connectionId={}, userId={}, institutionId={}",
                            conn.getId(), conn.getUserId(), conn.getInstitutionId());
                    syncOpenFinanceConnectionUseCase.execute(conn.getId(), null, OpenFinanceSyncType.SCHEDULED);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.error("Erro ao sincronizar conexão Open Finance agendada {}: {}", conn.getId(), e.getMessage(), e);
                }
            }

            log.info("Agendamento OpenFinanceSyncScheduler concluído. Sucesso: {}, Falhas: {}, Total: {}",
                    successCount, failureCount, activeConnections.size());
        } catch (Exception e) {
            log.error("Erro crítico no agendador OpenFinanceSyncScheduler: {}", e.getMessage(), e);
        }
    }
}
