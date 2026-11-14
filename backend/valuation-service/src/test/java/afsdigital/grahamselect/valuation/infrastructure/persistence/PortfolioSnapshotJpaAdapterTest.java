package afsdigital.grahamselect.valuation.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioSnapshotJpaAdapterTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query tradeQueryMock;

    @Mock
    private Query priceQueryMock;

    @InjectMocks
    private PortfolioSnapshotJpaAdapter portfolioSnapshotJpaAdapter;

    @Test
    void shouldCalculateCurrentAllocationCorrectly() {
        String userId = "user-123";

        // Setup mock trade query results
        Object[] row1 = new Object[]{"PETR4", new BigDecimal("100")};
        Object[] row2 = new Object[]{"VALE3", new BigDecimal("50")};
        List<Object[]> tradeResults = List.of(row1, row2);

        when(entityManager.createNativeQuery(contains("trades"))).thenReturn(tradeQueryMock);
        when(tradeQueryMock.setParameter("userId", userId)).thenReturn(tradeQueryMock);
        when(tradeQueryMock.getResultList()).thenReturn(tradeResults);

        // Setup mock price query results
        Object[] priceRow1 = new Object[]{"PETR4", new BigDecimal("30.00")};
        Object[] priceRow2 = new Object[]{"VALE3", new BigDecimal("80.00")};
        List<Object[]> priceResults = List.of(priceRow1, priceRow2);

        when(entityManager.createNativeQuery(contains("stock_price"))).thenReturn(priceQueryMock);
        when(priceQueryMock.setParameter(eq("tickers"), any())).thenReturn(priceQueryMock);
        when(priceQueryMock.getResultList()).thenReturn(priceResults);

        // Execute
        Map<String, BigDecimal> allocation = portfolioSnapshotJpaAdapter.getCurrentAllocationByUserId(userId);

        // Assertions
        // Total value = 100 * 30 + 50 * 80 = 3000 + 4000 = 7000
        // PETR4 allocation = (3000 / 7000) * 100 = 42.8571%
        // VALE3 allocation = (4000 / 7000) * 100 = 57.1429%
        assertThat(allocation).hasSize(2);
        assertThat(allocation.get("PETR4")).isEqualByComparingTo("42.8571");
        assertThat(allocation.get("VALE3")).isEqualByComparingTo("57.1429");
    }

    @Test
    void shouldReturnEmptyMapWhenNoTradesFound() {
        String userId = "user-123";
        when(entityManager.createNativeQuery(contains("trades"))).thenReturn(tradeQueryMock);
        when(tradeQueryMock.setParameter("userId", userId)).thenReturn(tradeQueryMock);
        when(tradeQueryMock.getResultList()).thenReturn(List.of());

        Map<String, BigDecimal> allocation = portfolioSnapshotJpaAdapter.getCurrentAllocationByUserId(userId);

        assertThat(allocation).isEmpty();
    }

    @Test
    void shouldFilterSnapshotQueryByTagIdUsingSafeAlias() {
        String userId = "user-123";
        UUID tagId = UUID.randomUUID();
        Object[] row1 = new Object[]{"PETR4", new BigDecimal("100")};
        List<Object[]> tradeResults = java.util.Collections.singletonList(row1);
        Object[] priceRow1 = new Object[]{"PETR4", new BigDecimal("30.00")};
        List<Object[]> priceResults = java.util.Collections.singletonList(priceRow1);

        when(entityManager.createNativeQuery(contains("asset_tags ag"))).thenReturn(tradeQueryMock);
        when(tradeQueryMock.setParameter("userId", userId)).thenReturn(tradeQueryMock);
        when(tradeQueryMock.setParameter("tagId", tagId.toString())).thenReturn(tradeQueryMock);
        when(tradeQueryMock.getResultList()).thenReturn(tradeResults);

        when(entityManager.createNativeQuery(contains("stock_price"))).thenReturn(priceQueryMock);
        when(priceQueryMock.setParameter(eq("tickers"), any())).thenReturn(priceQueryMock);
        when(priceQueryMock.getResultList()).thenReturn(priceResults);

        Map<String, BigDecimal> allocation = portfolioSnapshotJpaAdapter.getCurrentAllocationByUserIdAndTagId(userId, tagId);

        verify(entityManager).createNativeQuery(argThat((String sql) ->
                sql.contains("asset_tags ag") && !sql.contains("asset_tags at")));
        verify(tradeQueryMock).setParameter("tagId", tagId.toString());
        assertThat(allocation).hasSize(1);
        assertThat(allocation.get("PETR4")).isEqualByComparingTo("100.0000");
    }
}
