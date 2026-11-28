package afsdigital.grahamselect.api.dividends.infrastructure.persistence;

import afsdigital.grahamselect.api.dividends.infrastructure.persistence.jpa.entities.DividendAnnouncementEntity;
import afsdigital.grahamselect.api.dividends.infrastructure.persistence.jpa.repositories.DividendAnnouncementJpaRepository;
import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DividendAnnouncementJpaAdapterTest {

    @Mock
    private DividendAnnouncementJpaRepository repository;

    @InjectMocks
    private DividendAnnouncementJpaAdapter adapter;

    @Test
    @DisplayName("Deve salvar novo anúncio de dividendo quando não existir prévio")
    void shouldSaveNewDividendAnnouncement() {
        DividendAnnouncement announcement = DividendAnnouncement.builder()
                .ticker("PETR4")
                .type(CorporateEventType.DIVIDENDO)
                .rate(new BigDecimal("1.250000"))
                .comDate(LocalDate.of(2026, 4, 15))
                .source("CVM")
                .status("APPROVED")
                .build();

        when(repository.findByTickerAndComDateAndTypeAndRate("PETR4", LocalDate.of(2026, 4, 15), "DIVIDENDO", new BigDecimal("1.250000")))
                .thenReturn(Optional.empty());

        when(repository.save(any(DividendAnnouncementEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DividendAnnouncement saved = adapter.save(announcement);

        assertThat(saved).isNotNull();
        assertThat(saved.getTicker()).isEqualTo("PETR4");
        assertThat(saved.getType()).isEqualTo(CorporateEventType.DIVIDENDO);
        verify(repository).save(any(DividendAnnouncementEntity.class));
    }

    @Test
    @DisplayName("Deve atualizar anúncio existente mantendo id original quando encontrar registro com mesma chave única composta")
    void shouldUpdateExistingAnnouncementDeterministically() {
        UUID existingId = UUID.randomUUID();
        LocalDate comDate = LocalDate.of(2026, 4, 15);
        BigDecimal rate = new BigDecimal("1.250000");

        DividendAnnouncementEntity existingEntity = DividendAnnouncementEntity.builder()
                .id(existingId)
                .ticker("PETR4")
                .type("DIVIDENDO")
                .rate(rate)
                .netValue(rate)
                .comDate(comDate)
                .paymentDate(null)
                .source("CVM")
                .status("APPROVED")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(5))
                .updatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(5))
                .isNew(false)
                .build();

        when(repository.findByTickerAndComDateAndTypeAndRate("PETR4", comDate, "DIVIDENDO", rate))
                .thenReturn(Optional.of(existingEntity));

        when(repository.save(any(DividendAnnouncementEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LocalDate newPaymentDate = LocalDate.of(2026, 5, 20);
        DividendAnnouncement updatedAnnouncement = DividendAnnouncement.builder()
                .ticker("PETR4")
                .type(CorporateEventType.DIVIDENDO)
                .rate(rate)
                .comDate(comDate)
                .paymentDate(newPaymentDate)
                .source("BRAPI")
                .status("PAID")
                .build();

        DividendAnnouncement result = adapter.save(updatedAnnouncement);

        assertThat(result.getId()).isEqualTo(existingId);
        assertThat(result.getPaymentDate()).isEqualTo(newPaymentDate);
        assertThat(result.getStatus()).isEqualTo("PAID");
        assertThat(existingEntity.getPaymentDate()).isEqualTo(newPaymentDate);
    }

    @Test
    @DisplayName("Deve passar Pageable limitado ao consultar agenda de dividendos")
    void shouldPassBoundedPageableToFindSchedule() {
        when(repository.findSchedule(any(), any(Boolean.class), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        adapter.findByTickersAndDateRange(List.of("PETR4"), start, end, "APPROVED");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findSchedule(
                any(),
                any(Boolean.class),
                any(),
                any(),
                any(),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(500);
    }
}
