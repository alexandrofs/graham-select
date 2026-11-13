package afsdigital.grahamselect.api.valuation.infrastructure.persistence;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaTradeRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CustodyTickerRepositoryImplTest {

    @Mock
    private JpaTradeRepository jpaTradeRepository;

    @Mock
    private CompanyJpaRepository companyJpaRepository;

    @InjectMocks
    private CustodyTickerRepositoryImpl custodyTickerRepository;

    @Test
    void shouldCombineAndSortTickersFromTradesAndCompanyMasterDataWithoutDuplicates() {
        // Arrange
        List<String> mockTradeTickers = List.of("PETR4", "VALE3");
        List<String> mockMasterTickers = List.of("VALE3", "XPTO3");
        when(jpaTradeRepository.findDistinctTickers()).thenReturn(mockTradeTickers);
        when(companyJpaRepository.findAllTickers()).thenReturn(mockMasterTickers);

        // Act
        List<String> result = custodyTickerRepository.findDistinctActiveTickers();

        // Assert
        assertEquals(List.of("PETR4", "VALE3", "XPTO3"), result);
        verify(jpaTradeRepository, times(1)).findDistinctTickers();
        verify(companyJpaRepository, times(1)).findAllTickers();
    }
}

