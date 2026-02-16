package afsdigital.grahamselect.valuation.application.service;

import afsdigital.grahamselect.valuation.application.repository.IntrinsicValueRepository;
import afsdigital.grahamselect.valuation.domain.entities.IntrinsicValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RankCompaniesServiceTest {

    @Mock
    private IntrinsicValueRepository intrinsicValueRepository;

    @InjectMocks
    private RankCompaniesService rankCompaniesService;

    @Test
    public void shouldReturnRankedCompanies() {
        List<IntrinsicValue> expectedRank = List.of(
                IntrinsicValue.builder().companyId("1").value(new BigDecimal("10.00")).build(),
                IntrinsicValue.builder().companyId("2").value(new BigDecimal("20.00")).build());

        when(intrinsicValueRepository.findTop20BestRanked()).thenReturn(expectedRank);

        List<IntrinsicValue> result = rankCompaniesService.rankCompanies();

        assertEquals(2, result.size());
        assertEquals(expectedRank, result);
        verify(intrinsicValueRepository, times(1)).findTop20BestRanked();
    }
}
