package afsdigital.grahamselect.common.dividends.application.usecase;

import afsdigital.grahamselect.common.dividends.application.dto.DividendScheduleDTO;
import afsdigital.grahamselect.common.dividends.application.repository.DividendAnnouncementPort;
import afsdigital.grahamselect.common.dividends.domain.entities.CorporateEventType;
import afsdigital.grahamselect.common.dividends.domain.entities.DividendAnnouncement;
import afsdigital.grahamselect.common.portfolio.application.dto.CustodyPositionDTO;
import afsdigital.grahamselect.common.portfolio.application.usecase.GetCustodyPositionsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDividendScheduleUseCaseTest {

    @Mock
    private DividendAnnouncementPort dividendAnnouncementPort;

    @Mock
    private GetCustodyPositionsUseCase getCustodyPositionsUseCase;

    @InjectMocks
    private GetDividendScheduleUseCase useCase;

    @Test
    @DisplayName("Deve retornar agenda e calcular estimatedTotal e estimatedNetTotal para usuário com custódia")
    void shouldCalculateEstimatedTotalsWhenUserHasShares() {
        String userId = "user-123";

        CustodyPositionDTO position = new CustodyPositionDTO(
                "PETR4",
                new BigDecimal("100.00"),
                new BigDecimal("30.00"),
                new BigDecimal("35.00"),
                new BigDecimal("3500.00"),
                new BigDecimal("16.67"),
                "LIVE",
                null,
                "Ações"
        );

        when(getCustodyPositionsUseCase.execute(userId)).thenReturn(List.of(position));

        DividendAnnouncement jcp = DividendAnnouncement.builder()
                .ticker("PETR4")
                .type(CorporateEventType.JCP)
                .rate(new BigDecimal("1.000000"))
                .comDate(LocalDate.of(2026, 4, 15))
                .paymentDate(LocalDate.of(2026, 5, 20))
                .source("BRAPI")
                .status("APPROVED")
                .build();

        when(dividendAnnouncementPort.findByTickersAndDateRange(any(), any(), any(), any()))
                .thenReturn(List.of(jcp));

        List<DividendScheduleDTO> schedule = useCase.execute(userId, "PETR4", null, null, null, true);

        assertThat(schedule).hasSize(1);
        DividendScheduleDTO item = schedule.get(0);
        assertThat(item.ticker()).isEqualTo("PETR4");
        assertThat(item.userShares()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(item.estimatedTotal()).isEqualByComparingTo(new BigDecimal("100.00")); // 100 * 1.00
        assertThat(item.estimatedNetTotal()).isEqualByComparingTo(new BigDecimal("85.00")); // 100 * 0.85
    }

    @Test
    @DisplayName("Deve retornar estimatedTotal nulo quando usuário não possuir custódia do ticker")
    void shouldReturnNullEstimatedTotalWhenUserHasNoShares() {
        String userId = "user-123";
        when(getCustodyPositionsUseCase.execute(userId)).thenReturn(List.of());

        DividendAnnouncement div = DividendAnnouncement.builder()
                .ticker("VALE3")
                .type(CorporateEventType.DIVIDENDO)
                .rate(new BigDecimal("2.500000"))
                .comDate(LocalDate.of(2026, 4, 15))
                .paymentDate(LocalDate.of(2026, 5, 20))
                .source("CVM")
                .status("APPROVED")
                .build();

        when(dividendAnnouncementPort.findByTickersAndDateRange(any(), any(), any(), any()))
                .thenReturn(List.of(div));

        List<DividendScheduleDTO> schedule = useCase.execute(userId, null, null, null, null, false);

        assertThat(schedule).hasSize(1);
        DividendScheduleDTO item = schedule.get(0);
        assertThat(item.estimatedTotal()).isNull();
        assertThat(item.estimatedNetTotal()).isNull();
    }
}
