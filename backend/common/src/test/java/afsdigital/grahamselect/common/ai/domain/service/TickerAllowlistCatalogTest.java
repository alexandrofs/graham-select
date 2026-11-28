package afsdigital.grahamselect.common.ai.domain.service;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TickerAllowlistCatalogTest {

    private final TickerAllowlistCatalog catalog = new TickerAllowlistCatalog();

    @Test
    void shouldNormalizeB3AndFractionalTickers() {
        assertThat(catalog.resolveCanonicalTicker("B3SA3")).contains("B3SA3");
        assertThat(catalog.resolveCanonicalTicker("petr4f")).contains("PETR4");
        assertThat(catalog.resolveCanonicalTicker("KLBN11F")).contains("KLBN11");
        assertThat(catalog.resolveCanonicalTicker("IVVB11")).contains("IVVB11");
        assertThat(catalog.resolveCanonicalTicker("HASH11")).contains("HASH11");
    }

    @Test
    void shouldValidateInternationalTickersWithoutStrippingNonB3F() {
        assertThat(catalog.resolveCanonicalTicker("AAPL")).contains("AAPL");
        assertThat(catalog.resolveCanonicalTicker("brk.b")).contains("BRK.B");
        assertThat(catalog.resolveCanonicalTicker("VOO")).contains("VOO");
        assertThat(catalog.resolveCanonicalTicker("AAPLF")).isEmpty();
    }

    @Test
    void shouldNormalizeTesouroDiretoPreservingValidYear() {
        assertThat(catalog.resolveCanonicalTicker("TESOURO_SELIC_2029")).contains("TESOURO_SELIC_2029");
        assertThat(catalog.resolveCanonicalTicker("Tesouro Selic 2029")).contains("TESOURO_SELIC_2029");
        assertThat(catalog.resolveCanonicalTicker("Tesouro IPCA+ 2035")).contains("TESOURO_IPCA_2035");
        assertThat(catalog.resolveCanonicalTicker("Tesouro Renda+ 2045")).contains("TESOURO_RENDA_MAIS_2045");
        assertThat(catalog.resolveCanonicalTicker("Tesouro Educa+ 2030")).contains("TESOURO_EDUCA_MAIS_2030");
        assertThat(catalog.resolveCanonicalTicker("TESOURO_PREFIXADO")).contains("TESOURO_PREFIXADO");
        assertThat(catalog.resolveCanonicalTicker("TESOURO_SELIC_1999")).isEmpty();
        assertThat(catalog.resolveCanonicalTicker("TESOURO_SELIC_2101")).isEmpty();
        assertThat(catalog.resolveCanonicalTicker("Tesouro Selic 2029 CPF 123.456.789-00")).isEmpty();
    }

    @Test
    void shouldNormalizeCryptoPairsToBaseSymbol() {
        assertThat(catalog.resolveCanonicalTicker("BTC-BRL")).contains("BTC");
        assertThat(catalog.resolveCanonicalTicker("btc-usd")).contains("BTC");
        assertThat(catalog.resolveCanonicalTicker("ETH")).contains("ETH");
    }

    @Test
    void shouldRejectBlankFreeTextAndLiteralManualAttempts() {
        assertThat(catalog.resolveCanonicalTicker(null)).isEmpty();
        assertThat(catalog.resolveCanonicalTicker("   ")).isEmpty();
        assertThat(catalog.resolveCanonicalTicker("CDB Banco Inter CPF 123.456.789-00")).isEmpty();
        assertThat(catalog.resolveCanonicalTicker("RENDA_FIXA_MANUAL_1")).isEmpty();
        assertThat(catalog.resolveCanonicalTicker("CRIPTO_MANUAL_2")).isEmpty();
    }

    @Test
    void shouldSupportExtraAllowedTickersInConstructorAndEnforceFormatValidation() {
        TickerAllowlistCatalog extended = new TickerAllowlistCatalog(Set.of("CUSTOM3", "IGNORED_MANUAL_1", "INVALID TICKER!"));
        assertThat(extended.resolveCanonicalTicker("CUSTOM3")).contains("CUSTOM3");
        assertThat(extended.resolveCanonicalTicker("IGNORED_MANUAL_1")).isEmpty();
        assertThat(extended.resolveCanonicalTicker("INVALID TICKER!")).isEmpty();
    }
}
