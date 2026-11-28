package afsdigital.grahamselect.common.cerrado.application.service;

import afsdigital.grahamselect.common.ai.application.port.AiDiagnosticCachePort;
import afsdigital.grahamselect.common.ai.application.service.AiDiagnosticCacheService;
import afsdigital.grahamselect.common.ai.domain.model.AiFinOpsProperties;
import afsdigital.grahamselect.common.cerrado.application.repository.CerradoPreferencesPort;
import afsdigital.grahamselect.common.cerrado.application.service.exceptions.CerradoPreferencesNotFoundException;
import afsdigital.grahamselect.common.cerrado.domain.exceptions.CerradoValidationException;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoPreferences;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoTemplate;
import afsdigital.grahamselect.common.cerrado.domain.service.CerradoAllocationEngine;
import afsdigital.grahamselect.common.cerrado.domain.service.CerradoTemplateCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CerradoPreferencesServiceTest {

    @Mock
    private CerradoPreferencesPort preferencesPort;

    @Mock
    private AiDiagnosticCachePort aiDiagnosticCachePort;

    private CerradoPreferencesService service;

    @BeforeEach
    void setUp() {
        CerradoAllocationEngine engine = new CerradoAllocationEngine();
        AiDiagnosticCacheService cacheService = new AiDiagnosticCacheService(
                aiDiagnosticCachePort,
                new AiFinOpsProperties(5, 30, 24)
        );
        CerradoTemplateCatalog catalog = new CerradoTemplateCatalog();
        service = new CerradoPreferencesService(preferencesPort, engine, cacheService, catalog);
    }

    @Test
    @DisplayName("getTemplates should return 3 official profiles summing 100.00%, defaultNeutralGrade=6 and CVM disclaimer")
    void shouldReturnOfficialTemplates() {
        CerradoPreferencesService.TemplatesCatalogResult result = service.getTemplates();

        assertThat(result.defaultNeutralGrade()).isEqualTo(6);
        assertThat(result.cvmDisclaimer()).contains("CVM");
        assertThat(result.templates()).hasSize(3);

        for (CerradoTemplate template : result.templates()) {
            BigDecimal sum = template.targetAcoesBr()
                    .add(template.targetFiis())
                    .add(template.targetRendaFixa())
                    .add(template.targetInternacional())
                    .add(template.targetCripto());
            assertThat(sum).isEqualByComparingTo("100.00");
        }

        assertThat(result.templates())
                .extracting(CerradoTemplate::code)
                .containsExactly("EQUILIBRADO_CERRADO", "MODERADO_PATRIMONIAL", "ACUMULACAO_ARROJADA");
    }

    @Test
    @DisplayName("savePreferences should insert with Graham defaults when omitted and invalidate AI cache")
    void shouldInsertWithGrahamDefaultsAndInvalidateCache() {
        when(preferencesPort.findByUserId(10L)).thenReturn(Optional.empty());
        when(preferencesPort.save(any())).thenAnswer(inv -> {
            CerradoPreferences p = inv.getArgument(0);
            return CerradoPreferences.builder()
                    .id(100L)
                    .userId(p.getUserId())
                    .targetAcoesBr(p.getTargetAcoesBr())
                    .targetFiis(p.getTargetFiis())
                    .targetRendaFixa(p.getTargetRendaFixa())
                    .targetInternacional(p.getTargetInternacional())
                    .targetCripto(p.getTargetCripto())
                    .grahamMultiplier(p.getGrahamMultiplier())
                    .grahamFilterEnabled(p.isGrahamFilterEnabled())
                    .updatedAt(p.getUpdatedAt())
                    .build();
        });

        CerradoPreferences saved = service.savePreferences(
                10L,
                new BigDecimal("39.00"),
                new BigDecimal("30.00"),
                new BigDecimal("20.00"),
                new BigDecimal("10.00"),
                new BigDecimal("1.00"),
                null,
                null
        );

        assertThat(saved.getId()).isEqualTo(100L);
        assertThat(saved.getGrahamMultiplier()).isEqualByComparingTo("1.30");
        assertThat(saved.isGrahamFilterEnabled()).isTrue();
        assertThat(saved.getUpdatedAt()).isNotNull();

        verify(aiDiagnosticCachePort).deleteByUserId(10L);
    }

    @Test
    @DisplayName("savePreferences should preserve existing id and Graham settings on update when omitted")
    void shouldPreserveGrahamSettingsOnUpdateWhenOmitted() {
        CerradoPreferences existing = CerradoPreferences.builder()
                .id(55L)
                .userId(10L)
                .targetAcoesBr(new BigDecimal("39.00"))
                .targetFiis(new BigDecimal("30.00"))
                .targetRendaFixa(new BigDecimal("20.00"))
                .targetInternacional(new BigDecimal("10.00"))
                .targetCripto(new BigDecimal("1.00"))
                .grahamMultiplier(new BigDecimal("1.75"))
                .grahamFilterEnabled(false)
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(1))
                .build();

        when(preferencesPort.findByUserId(10L)).thenReturn(Optional.of(existing));
        when(preferencesPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CerradoPreferences updated = service.savePreferences(
                10L,
                new BigDecimal("45"),
                new BigDecimal("25"),
                new BigDecimal("10"),
                new BigDecimal("15"),
                new BigDecimal("5"),
                null,
                null
        );

        ArgumentCaptor<CerradoPreferences> captor = ArgumentCaptor.forClass(CerradoPreferences.class);
        verify(preferencesPort).save(captor.capture());
        CerradoPreferences persisted = captor.getValue();

        assertThat(persisted.getId()).isEqualTo(55L);
        assertThat(persisted.getGrahamMultiplier()).isEqualByComparingTo("1.75");
        assertThat(persisted.isGrahamFilterEnabled()).isFalse();
        assertThat(updated.getTargetAcoesBr()).isEqualByComparingTo("45.00");
        verify(aiDiagnosticCachePort).deleteByUserId(10L);
    }

    @Test
    @DisplayName("savePreferences should throw CerradoValidationException on invalid targets or grahamMultiplier without persisting or invalidating cache")
    void shouldRejectInvalidInputsWithCerradoValidationException() {
        when(preferencesPort.findByUserId(10L)).thenReturn(Optional.empty());

        // Sum != 100
        assertThatThrownBy(() -> service.savePreferences(
                10L,
                new BigDecimal("38.00"),
                new BigDecimal("30.00"),
                new BigDecimal("20.00"),
                new BigDecimal("10.00"),
                new BigDecimal("0.00"),
                null,
                null
        )).isInstanceOf(CerradoValidationException.class);

        // Null target -> must throw CerradoValidationException (422), NOT IllegalArgumentException (400)
        assertThatThrownBy(() -> service.savePreferences(
                10L,
                null,
                new BigDecimal("30.00"),
                new BigDecimal("40.00"),
                new BigDecimal("20.00"),
                new BigDecimal("10.00"),
                null,
                null
        )).isInstanceOf(CerradoValidationException.class);

        // Scale > 2
        assertThatThrownBy(() -> service.savePreferences(
                10L,
                new BigDecimal("33.333"),
                new BigDecimal("26.667"),
                new BigDecimal("20.00"),
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                null,
                null
        )).isInstanceOf(CerradoValidationException.class);

        // grahamMultiplier > 99.99
        assertThatThrownBy(() -> service.savePreferences(
                10L,
                new BigDecimal("39.00"),
                new BigDecimal("30.00"),
                new BigDecimal("20.00"),
                new BigDecimal("10.00"),
                new BigDecimal("1.00"),
                new BigDecimal("100.00"),
                true
        )).isInstanceOf(CerradoValidationException.class);

        // grahamMultiplier <= 0
        assertThatThrownBy(() -> service.savePreferences(
                10L,
                new BigDecimal("39.00"),
                new BigDecimal("30.00"),
                new BigDecimal("20.00"),
                new BigDecimal("10.00"),
                new BigDecimal("1.00"),
                BigDecimal.ZERO,
                true
        )).isInstanceOf(CerradoValidationException.class);

        // grahamMultiplier scale > 2
        assertThatThrownBy(() -> service.savePreferences(
                10L,
                new BigDecimal("39.00"),
                new BigDecimal("30.00"),
                new BigDecimal("20.00"),
                new BigDecimal("10.00"),
                new BigDecimal("1.00"),
                new BigDecimal("1.333"),
                true
        )).isInstanceOf(CerradoValidationException.class);

        // Negative target
        assertThatThrownBy(() -> service.savePreferences(
                10L,
                new BigDecimal("-1.00"),
                new BigDecimal("31.00"),
                new BigDecimal("40.00"),
                new BigDecimal("20.00"),
                new BigDecimal("10.00"),
                null,
                null
        )).isInstanceOf(CerradoValidationException.class);

        // Target > 100
        assertThatThrownBy(() -> service.savePreferences(
                10L,
                new BigDecimal("101.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                new BigDecimal("0.00"),
                null,
                null
        )).isInstanceOf(CerradoValidationException.class);

        verify(preferencesPort, never()).save(any());
        verify(aiDiagnosticCachePort, never()).deleteByUserId(any());
    }

    @Test
    @DisplayName("getPreferences and savePreferences should reject null userId")
    void shouldRejectNullUserId() {
        assertThatThrownBy(() -> service.getPreferences(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.savePreferences(
                null,
                new BigDecimal("39.00"),
                new BigDecimal("30.00"),
                new BigDecimal("20.00"),
                new BigDecimal("10.00"),
                new BigDecimal("1.00"),
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getPreferences should throw CerradoPreferencesNotFoundException when absent")
    void shouldThrowNotFoundWhenPreferencesAbsent() {
        when(preferencesPort.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPreferences(99L))
                .isInstanceOf(CerradoPreferencesNotFoundException.class);
    }
}
