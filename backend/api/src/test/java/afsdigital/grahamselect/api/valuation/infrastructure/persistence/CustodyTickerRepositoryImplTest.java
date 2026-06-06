package afsdigital.grahamselect.api.valuation.infrastructure.persistence;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaTradeRepository;
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

    @InjectMocks
    private CustodyTickerRepositoryImpl custodyTickerRepository;

    @Test
    void shouldDelegateToJpaRepository() {
        // Arrange
        List<String> mockTickers = List.of("PETR4", "VALE3", "ITUB4");
        when(jpaTradeRepository.findDistinctTickers()).thenReturn(mockTickers);

        // Act
        List<String> result = custodyTickerRepository.findDistinctActiveTickers();

        // Assert
        assertEquals(mockTickers, result);
        verify(jpaTradeRepository, times(1)).findDistinctTickers();
    }
}
