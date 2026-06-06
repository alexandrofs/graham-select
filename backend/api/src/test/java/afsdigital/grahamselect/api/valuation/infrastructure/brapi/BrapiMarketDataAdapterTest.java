package afsdigital.grahamselect.api.valuation.infrastructure.brapi;

import afsdigital.grahamselect.valuation.application.dto.MarketDataResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class BrapiMarketDataAdapterTest {

    private BrapiMarketDataAdapter adapter;
    private MockRestServiceServer mockServer;
    private CacheManager cacheManager;
    private Cache cache;

    @BeforeEach
    public void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://brapi.dev/api");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        when(cacheManager.getCache("market-data")).thenReturn(cache);

        adapter = new BrapiMarketDataAdapter(restClient, cacheManager, "test-token");
    }

    @Test
    public void shouldReturnMarketDataResultWhenResponseIs200() {
        // Arrange
        String responseJson = """
                {
                  "results": [
                    {
                      "symbol": "PETR4",
                      "regularMarketPrice": 38.45,
                      "dividendYield": 8.2,
                      "priceEarnings": 5.1,
                      "priceToBook": 1.8,
                      "defaultKeyStatistics": {
                        "earningsPerShare": { "raw": 7.54 },
                        "bookValue": { "raw": 21.36 }
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://brapi.dev/api/quote/PETR4?modules=summaryProfile,financialData&token=test-token"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        // Act
        List<MarketDataResult> results = adapter.fetchMarketData(List.of("PETR4"));

        // Assert
        mockServer.verify();
        assertEquals(1, results.size());
        MarketDataResult res = results.get(0);
        assertEquals("PETR4", res.ticker());
        assertEquals(BigDecimal.valueOf(38.45), res.price());
        assertEquals(8.2, res.dividendYield());
        assertEquals(5.1, res.priceEarnings());
        assertEquals(1.8, res.priceToBook());
        assertEquals(7.54, res.earningsPerShare());
        assertEquals(21.36, res.bookValuePerShare());

        verify(cache).put(eq("PETR4"), any(MarketDataResult.class));
    }

    @Test
    public void shouldFallbackToCacheWhenApiFails() {
        // Arrange
        mockServer.expect(requestTo("https://brapi.dev/api/quote/PETR4?modules=summaryProfile,financialData&token=test-token"))
                .andRespond(withServerError());

        MarketDataResult cachedResult = new MarketDataResult(
                "PETR4", BigDecimal.valueOf(38.00), 8.0, 5.0, 1.7, 7.50, 21.00
        );
        when(cache.get("PETR4", MarketDataResult.class)).thenReturn(cachedResult);

        // Act
        List<MarketDataResult> results = adapter.fetchMarketData(List.of("PETR4"));

        // Assert
        mockServer.verify();
        assertEquals(1, results.size());
        MarketDataResult res = results.get(0);
        assertEquals("PETR4", res.ticker());
        assertEquals(BigDecimal.valueOf(38.00), res.price());
    }

    @Test
    public void shouldSkipInvalidTickerInResponse() {
        // Arrange
        String responseJson = """
                {
                  "results": [
                    {
                      "symbol": "PETR4",
                      "regularMarketPrice": null,
                      "dividendYield": null,
                      "priceEarnings": null,
                      "priceToBook": null,
                      "defaultKeyStatistics": null
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://brapi.dev/api/quote/PETR4?modules=summaryProfile,financialData&token=test-token"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        // Act
        List<MarketDataResult> results = adapter.fetchMarketData(List.of("PETR4"));

        // Assert
        mockServer.verify();
        assertTrue(results.isEmpty());
        verify(cache, never()).put(any(), any());
    }
}
