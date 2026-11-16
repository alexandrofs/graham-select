package afsdigital.grahamselect.common.dividends.domain.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DividendAnnouncementTest {

    @Test
    @DisplayName("Deve calcular netValue corretamente com desconto de 15% de IRRF para JCP")
    void shouldCalculateNetValueForJcp() {
        BigDecimal rate = new BigDecimal("1.000000");
        DividendAnnouncement announcement = DividendAnnouncement.builder()
                .ticker("PETR4")
                .type(CorporateEventType.JCP)
                .rate(rate)
                .comDate(LocalDate.now(ZoneOffset.UTC))
                .source("CVM")
                .status("APPROVED")
                .build();

        assertThat(announcement.getNetValue()).isEqualByComparingTo("0.850000");
    }

    @Test
    @DisplayName("Deve manter netValue igual ao rate bruto para DIVIDENDO e RENDIMENTO (isentos de IRRF)")
    void shouldCalculateNetValueForDividendoAndRendimento() {
        BigDecimal rate = new BigDecimal("1.500000");
        DividendAnnouncement dividendo = DividendAnnouncement.builder()
                .ticker("VALE3")
                .type(CorporateEventType.DIVIDENDO)
                .rate(rate)
                .comDate(LocalDate.now(ZoneOffset.UTC))
                .source("CVM")
                .status("APPROVED")
                .build();

        assertThat(dividendo.getNetValue()).isEqualByComparingTo("1.500000");

        DividendAnnouncement rendimento = DividendAnnouncement.builder()
                .ticker("HGLG11")
                .type(CorporateEventType.RENDIMENTO)
                .rate(new BigDecimal("1.100000"))
                .comDate(LocalDate.now(ZoneOffset.UTC))
                .source("B3")
                .status("APPROVED")
                .build();

        assertThat(rendimento.getNetValue()).isEqualByComparingTo("1.100000");
    }

    @Test
    @DisplayName("Deve validar campos obrigatórios do anúncio de proventos")
    void shouldValidateMandatoryFields() {
        assertThatThrownBy(() -> DividendAnnouncement.builder()
                .type(CorporateEventType.DIVIDENDO)
                .rate(BigDecimal.ONE)
                .comDate(LocalDate.now(ZoneOffset.UTC))
                .source("CVM")
                .status("APPROVED")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ticker");
    }
}
