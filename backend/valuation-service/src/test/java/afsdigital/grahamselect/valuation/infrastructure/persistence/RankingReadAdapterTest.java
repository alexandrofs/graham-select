package afsdigital.grahamselect.valuation.infrastructure.persistence;

import afsdigital.grahamselect.valuation.domain.entities.RankedCompany;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingReadAdapterTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query queryMock;

    @InjectMocks
    private RankingReadAdapter rankingReadAdapter;

    @Test
    void shouldFindTop20BestRankedCorrectly() {
        Object[] row1 = new Object[]{"PETR4", "Petrobras", new BigDecimal("40.00"), new BigDecimal("30.00"), new BigDecimal("0.3333")};
        Object[] row2 = new Object[]{"VALE3", "Vale", new BigDecimal("80.00"), new BigDecimal("70.00"), new BigDecimal("0.1428")};
        List<Object[]> results = List.of(row1, row2);

        when(entityManager.createNativeQuery(anyString())).thenReturn(queryMock);
        when(queryMock.getResultList()).thenReturn(results);

        List<RankedCompany> ranked = rankingReadAdapter.findTop20BestRanked();

        assertThat(ranked).hasSize(2);
        assertThat(ranked.get(0).getSymbol()).isEqualTo("PETR4");
        assertThat(ranked.get(0).getName()).isEqualTo("Petrobras");
        assertThat(ranked.get(0).getIntrinsicValue()).isEqualByComparingTo("40.00");
        assertThat(ranked.get(0).getCurrentPrice()).isEqualByComparingTo("30.00");
        assertThat(ranked.get(0).getMarginOfSafety()).isEqualByComparingTo("0.3333");
    }
}
