package afsdigital.grahamselect.api.admin.application.service;

import afsdigital.grahamselect.api.admin.application.dto.AdminObservabilityDto;
import afsdigital.grahamselect.api.admin.application.dto.AnonymizedUploadTraceDto;
import afsdigital.grahamselect.api.upload.infrastructure.persistence.jpa.entities.B3ImportStatusEntity;
import afsdigital.grahamselect.api.upload.infrastructure.persistence.jpa.repository.B3ImportStatusJpaRepository;
import afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.entities.IngestionAudit;
import afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.repositories.IngestionAuditRepository;
import afsdigital.grahamselect.common.upload.domain.model.B3ImportStatusState;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminObservabilityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private IngestionAuditRepository ingestionAuditRepository;

    @Mock
    private B3ImportStatusJpaRepository b3ImportStatusJpaRepository;

    private SimpleMeterRegistry meterRegistry;
    private AdminObservabilityService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new AdminObservabilityService(
                userRepository,
                ingestionAuditRepository,
                b3ImportStatusJpaRepository,
                meterRegistry
        );
    }

    @Test
    @DisplayName("Should calculate active users, deduplicate uploads by correlationId, and anonymize traces")
    void shouldCalculateMetricsAndAnonymizeUploads() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        when(userRepository.findActiveUserIdsSince(any())).thenReturn(List.of(1L, 2L));
        when(userRepository.countActiveUsersSince(any())).thenReturn(2L);
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByTier(SubscriptionTier.TRIAL)).thenReturn(3L);
        when(userRepository.countByTier(SubscriptionTier.FREE)).thenReturn(5L);
        when(userRepository.countByTier(SubscriptionTier.PREMIUM)).thenReturn(2L);
        when(ingestionAuditRepository.findDistinctUserIdsSince(any())).thenReturn(List.of("2", "3"));

        B3ImportStatusEntity b3Upload1 = B3ImportStatusEntity.builder()
                .id(10L)
                .correlationId("corr-1")
                .userId("1")
                .fileName("Joao_Silva_CPF_12345678900.xlsx")
                .status(B3ImportStatusState.COMPLETED)
                .processedRows(50)
                .successfulRows(50)
                .failedRows(0)
                .createdAt(now.minusHours(2))
                .updatedAt(now.minusHours(2))
                .build();

        B3ImportStatusEntity b3Upload2 = B3ImportStatusEntity.builder()
                .id(11L)
                .correlationId("corr-2")
                .userId("2")
                .fileName("carteira_maria.xlsx")
                .status(B3ImportStatusState.FAILED)
                .processedRows(10)
                .successfulRows(0)
                .failedRows(10)
                .createdAt(now.minusHours(1))
                .updatedAt(now.minusHours(1))
                .build();

        // Duplicate correlationId "corr-1" in IngestionAudit should be deduplicated
        IngestionAudit audit1 = IngestionAudit.builder()
                .id(100L)
                .correlationId("corr-1")
                .userId("1")
                .fileName("Joao_Silva_CPF_12345678900.xlsx")
                .status(IngestionAudit.IngestionStatus.SUCESSO)
                .totalLines(50)
                .processedLines(50)
                .errorLines(0)
                .uploadDate(now.minusHours(2))
                .createdAt(now.minusHours(2))
                .build();

        IngestionAudit audit3 = IngestionAudit.builder()
                .id(101L)
                .correlationId("corr-3")
                .userId("3")
                .fileName("extrato.csv")
                .status(IngestionAudit.IngestionStatus.PARCIAL)
                .totalLines(20)
                .processedLines(18)
                .errorLines(2)
                .uploadDate(now.minusMinutes(30))
                .createdAt(now.minusMinutes(30))
                .build();

        when(b3ImportStatusJpaRepository.findAll()).thenReturn(List.of(b3Upload1, b3Upload2));
        when(ingestionAuditRepository.findAll()).thenReturn(List.of(audit1, audit3));

        AdminObservabilityDto dto = service.getObservabilityMetrics();

        // User metrics: union of {1, 2} and {2, 3} = 3 active users
        assertThat(dto.userMetrics().activeUsers24h()).isEqualTo(3L);
        assertThat(dto.userMetrics().totalUsers()).isEqualTo(10L);
        assertThat(dto.userMetrics().trialUsers()).isEqualTo(3L);
        assertThat(dto.userMetrics().freeUsers()).isEqualTo(5L);
        assertThat(dto.userMetrics().premiumUsers()).isEqualTo(2L);

        // Upload metrics: 3 unique correlationIds (corr-1, corr-2, corr-3)
        assertThat(dto.uploadMetrics().totalUploads()).isEqualTo(3L);
        assertThat(dto.uploadMetrics().successCount()).isEqualTo(1L);
        assertThat(dto.uploadMetrics().partialCount()).isEqualTo(1L);
        assertThat(dto.uploadMetrics().errorCount()).isEqualTo(1L);
        assertThat(dto.uploadMetrics().errorRatePercentage()).isEqualByComparingTo(new BigDecimal("33.33"));

        // Anonymization checks
        assertThat(dto.recentUploads()).hasSize(3);
        for (AnonymizedUploadTraceDto trace : dto.recentUploads()) {
            assertThat(trace.anonymizedUserId()).matches("^usr_\\*\\*\\*[0-9a-f]{4}$");
            assertThat(trace.anonymizedUserId()).doesNotContain("Joao", "Silva", "12345678900");
            assertThat(trace.sanitizedFileName()).startsWith("extrato_b3_***.");
            assertThat(trace.sanitizedFileName()).doesNotContain("Joao", "maria");
            assertThat(trace.maskedPortfolioValue()).isEqualTo("***");
        }
    }

    @Test
    @DisplayName("Should read Graham latency from MeterRegistry when Timer has measurements")
    void shouldReadLatencyFromMeterRegistry() {
        when(b3ImportStatusJpaRepository.findAll()).thenReturn(List.of());
        when(ingestionAuditRepository.findAll()).thenReturn(List.of());

        Timer timer = Timer.builder("http.server.requests")
                .tag("uri", "/api/v1/graham-recommendations")
                .register(meterRegistry);
        timer.record(240, TimeUnit.MILLISECONDS);
        timer.record(360, TimeUnit.MILLISECONDS);

        AdminObservabilityDto dto = service.getObservabilityMetrics();

        assertThat(dto.grahamLatencyMetrics().averageLatencyMs()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(dto.grahamLatencyMetrics().slaTargetMs()).isEqualTo(2000L);
        assertThat(dto.grahamLatencyMetrics().withinSla()).isTrue();
        assertThat(dto.grahamLatencyMetrics().latencyHistory()).hasSize(6);
    }

    @Test
    @DisplayName("Should return deterministic operational baseline latency when MeterRegistry is empty")
    void shouldReturnFallbackLatencyWhenMeterRegistryEmpty() {
        when(b3ImportStatusJpaRepository.findAll()).thenReturn(List.of());
        when(ingestionAuditRepository.findAll()).thenReturn(List.of());

        AdminObservabilityDto dto = service.getObservabilityMetrics();

        assertThat(dto.grahamLatencyMetrics().averageLatencyMs()).isEqualByComparingTo(new BigDecimal("145.00"));
        assertThat(dto.grahamLatencyMetrics().p95LatencyMs()).isEqualByComparingTo(new BigDecimal("320.00"));
        assertThat(dto.grahamLatencyMetrics().withinSla()).isTrue();
        assertThat(dto.grahamLatencyMetrics().latencyHistory()).hasSize(6);
    }
}
