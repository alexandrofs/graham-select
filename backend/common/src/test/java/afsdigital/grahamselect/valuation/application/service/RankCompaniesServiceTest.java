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
    public void shouldReturnEmptyListWhenRankingIsPostponed() {
        List<IntrinsicValue> result = rankCompaniesService.rankCompanies();

        assertEquals(0, result.size());
        verifyNoInteractions(intrinsicValueRepository);
    }
}
