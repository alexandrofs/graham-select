package afsdigital.grahamselect.valuation.application.usecase;

import afsdigital.grahamselect.common.domain.entities.ExcludedTicker;
import afsdigital.grahamselect.valuation.application.dto.AllocationGoalDto;
import afsdigital.grahamselect.valuation.application.dto.GenerationResult;
import afsdigital.grahamselect.valuation.application.repository.AllocationGoalPort;
import afsdigital.grahamselect.valuation.application.repository.GrahamRecommendationPort;
import afsdigital.grahamselect.valuation.application.repository.PortfolioSnapshotPort;
import afsdigital.grahamselect.valuation.application.repository.RankingRepository;
import afsdigital.grahamselect.valuation.domain.entities.GrahamRecommendation;
import afsdigital.grahamselect.valuation.domain.entities.RankedCompany;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GenerateGrahamRecommendationsUseCaseTest {

    @Mock
    private AllocationGoalPort allocationGoalPort;

    @Mock
    private RankingRepository rankingRepository;

    @Mock
    private PortfolioSnapshotPort portfolioSnapshotPort;

    @Mock
    private GrahamRecommendationPort grahamRecommendationPort;

    @InjectMocks
    private GenerateGrahamRecommendationsUseCase generateGrahamRecommendationsUseCase;

    @Test
    public void shouldGenerateRecommendationsCorrectlyForUserWithGoals() {
        String userId = "user-123";
        LocalDate now = LocalDate.now(ZoneOffset.UTC);

        // Setup ranked companies
        RankedCompany petr4 = RankedCompany.builder()
                .symbol("PETR4")
                .name("Petrobras")
                .intrinsicValue(new BigDecimal("40.00"))
                .currentPrice(new BigDecimal("30.00"))
                .marginOfSafety(new BigDecimal("0.33")) // 33%
                .intrinsicValueUpdatedAt(now)
                .build();

        RankedCompany vale3 = RankedCompany.builder()
                .symbol("VALE3")
                .name("Vale")
                .intrinsicValue(new BigDecimal("80.00"))
                .currentPrice(new BigDecimal("70.00"))
                .marginOfSafety(new BigDecimal("0.14")) // 14%
                .intrinsicValueUpdatedAt(now)
                .build();

        RankedCompany negativeMos = RankedCompany.builder()
                .symbol("WEGE3")
                .name("Weg")
                .intrinsicValue(new BigDecimal("20.00"))
                .currentPrice(new BigDecimal("25.00"))
                .marginOfSafety(new BigDecimal("-0.20")) // -20%
                .intrinsicValueUpdatedAt(now)
                .build();

        when(rankingRepository.findTop20BestRanked()).thenReturn(List.of(petr4, vale3, negativeMos));

        // Setup goals: PETR4 meta is 20%, VALE3 meta is 10%
        when(allocationGoalPort.findByUserId(userId)).thenReturn(List.of(
                new AllocationGoalDto("TICKER", "PETR4", new BigDecimal("20.00")),
                new AllocationGoalDto("TICKER", "VALE3", new BigDecimal("10.00"))
        ));

        // Setup portfolio snapshot: PETR4 is 5%, VALE3 is 8% (so it is below target 10%)
        // PETR4 gap: 20 - 5 = 15
        // VALE3 gap: 10 - 8 = 2
        when(portfolioSnapshotPort.getCurrentAllocationByUserId(userId)).thenReturn(Map.of(
                "PETR4", new BigDecimal("5.00"),
                "VALE3", new BigDecimal("8.00")
        ));
 
        GenerationResult generationResult = generateGrahamRecommendationsUseCase.execute(userId);
        List<GrahamRecommendation> result = generationResult.recommendations();
 
        // Assertions
        assertNotNull(result);
        assertEquals(2, result.size()); // WEGE3 filtered out because marginOfSafety <= 0
        assertTrue(generationResult.excludedTickers().isEmpty());
 
        // Scores calculation check:
        // PETR4: marginOfSafety (0.33) * 0.6 + max(gap, 0)/100 (0.15) * 0.4 = 0.198 + 0.060 = 0.258
        // VALE3: marginOfSafety (0.14) * 0.6 + max(gap, 0)/100 (0.02) * 0.4 = 0.084 + 0.008 = 0.092
        // Sorted: PETR4 first, VALE3 second
        assertEquals("PETR4", result.get(0).getTicker());
        assertEquals("VALE3", result.get(1).getTicker());
 
        // Validate values mapped
        GrahamRecommendation petrRec = result.get(0);
        assertTrue(new BigDecimal("5.00").compareTo(petrRec.getCurrentAllocationPct()) == 0);
        assertTrue(new BigDecimal("20.00").compareTo(petrRec.getTargetAllocationPct()) == 0);
        assertTrue(new BigDecimal("15.00").compareTo(petrRec.getAllocationGap()) == 0);
        assertTrue(new BigDecimal("0.2580").compareTo(petrRec.getRecommendationScore()) == 0);
 
        GrahamRecommendation valeRec = result.get(1);
        assertTrue(new BigDecimal("8.00").compareTo(valeRec.getCurrentAllocationPct()) == 0);
        assertTrue(new BigDecimal("10.00").compareTo(valeRec.getTargetAllocationPct()) == 0);
        assertTrue(new BigDecimal("2.00").compareTo(valeRec.getAllocationGap()) == 0);
        assertTrue(new BigDecimal("0.0920").compareTo(valeRec.getRecommendationScore()) == 0);
 
        verify(grahamRecommendationPort, times(1)).saveRecommendations(eq(userId), anyList());
    }

    @Test
    public void shouldGenerateRecommendationsWithoutGoals() {
        String userId = "user-123";
        LocalDate now = LocalDate.now(ZoneOffset.UTC);

        RankedCompany petr4 = RankedCompany.builder()
                .symbol("PETR4")
                .name("Petrobras")
                .intrinsicValue(new BigDecimal("40.00"))
                .currentPrice(new BigDecimal("30.00"))
                .marginOfSafety(new BigDecimal("0.33"))
                .intrinsicValueUpdatedAt(now)
                .build();

        when(rankingRepository.findTop20BestRanked()).thenReturn(List.of(petr4));
        when(allocationGoalPort.findByUserId(userId)).thenReturn(List.of());
        when(portfolioSnapshotPort.getCurrentAllocationByUserId(userId)).thenReturn(Map.of());

        GenerationResult generationResult = generateGrahamRecommendationsUseCase.execute(userId);
        List<GrahamRecommendation> result = generationResult.recommendations();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(generationResult.excludedTickers().isEmpty());

        // PETR4 score: marginOfSafety (0.33) * 0.6 + 0 * 0.4 = 0.198
        GrahamRecommendation rec = result.get(0);
        assertTrue(new BigDecimal("0.198").compareTo(rec.getRecommendationScore()) == 0);
    }

    @Test
    public void shouldReturnEmptyListWhenNoRankedCompaniesFound() {
        String userId = "user-123";

        when(rankingRepository.findTop20BestRanked()).thenReturn(List.of());

        GenerationResult generationResult = generateGrahamRecommendationsUseCase.execute(userId);

        assertNotNull(generationResult.recommendations());
        assertTrue(generationResult.recommendations().isEmpty());
        assertTrue(generationResult.excludedTickers().isEmpty());
        verifyNoInteractions(allocationGoalPort, portfolioSnapshotPort, grahamRecommendationPort);
    }

    @Test
    public void shouldExcludeStaleData() {
        String userId = "user-123";
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        LocalDate tenDaysAgo = now.minusDays(10);

        RankedCompany stale = RankedCompany.builder()
                .symbol("MGLU3")
                .name("Magalu")
                .intrinsicValue(new BigDecimal("10.00"))
                .currentPrice(new BigDecimal("5.00"))
                .marginOfSafety(new BigDecimal("1.00"))
                .intrinsicValueUpdatedAt(tenDaysAgo)
                .build();

        RankedCompany fresh = RankedCompany.builder()
                .symbol("PETR4")
                .name("Petrobras")
                .intrinsicValue(new BigDecimal("40.00"))
                .currentPrice(new BigDecimal("30.00"))
                .marginOfSafety(new BigDecimal("0.33"))
                .intrinsicValueUpdatedAt(now)
                .build();

        when(rankingRepository.findTop20BestRanked()).thenReturn(List.of(stale, fresh));
        when(allocationGoalPort.findByUserId(userId)).thenReturn(List.of());
        when(portfolioSnapshotPort.getCurrentAllocationByUserId(userId)).thenReturn(Map.of());

        GenerationResult generationResult = generateGrahamRecommendationsUseCase.execute(userId);

        assertEquals(1, generationResult.recommendations().size());
        assertEquals("PETR4", generationResult.recommendations().get(0).getTicker());
        assertEquals(1, generationResult.excludedTickers().size());
        assertEquals("MGLU3", generationResult.excludedTickers().get(0).ticker());
        assertEquals("STALE_DATA", generationResult.excludedTickers().get(0).reason());
    }

    @Test
    public void shouldExcludeOutlierPrice() {
        String userId = "user-123";
        LocalDate now = LocalDate.now(ZoneOffset.UTC);

        RankedCompany outlier = RankedCompany.builder()
                .symbol("XPTO3")
                .name("Outlier")
                .intrinsicValue(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("10.00"))
                .marginOfSafety(new BigDecimal("9.00")) // 900%
                .intrinsicValueUpdatedAt(now)
                .build();

        RankedCompany normal = RankedCompany.builder()
                .symbol("PETR4")
                .name("Petrobras")
                .intrinsicValue(new BigDecimal("40.00"))
                .currentPrice(new BigDecimal("30.00"))
                .marginOfSafety(new BigDecimal("0.33"))
                .intrinsicValueUpdatedAt(now)
                .build();

        when(rankingRepository.findTop20BestRanked()).thenReturn(List.of(outlier, normal));
        when(allocationGoalPort.findByUserId(userId)).thenReturn(List.of());
        when(portfolioSnapshotPort.getCurrentAllocationByUserId(userId)).thenReturn(Map.of());

        GenerationResult generationResult = generateGrahamRecommendationsUseCase.execute(userId);

        assertEquals(1, generationResult.recommendations().size());
        assertEquals("PETR4", generationResult.recommendations().get(0).getTicker());
        assertEquals(1, generationResult.excludedTickers().size());
        assertEquals("XPTO3", generationResult.excludedTickers().get(0).ticker());
        assertEquals("OUTLIER_PRICE", generationResult.excludedTickers().get(0).reason());
    }

    @Test
    public void shouldPrioritizeStaleDataOverOutlier() {
        String userId = "user-123";
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        LocalDate tenDaysAgo = now.minusDays(10);

        RankedCompany both = RankedCompany.builder()
                .symbol("ABCD4")
                .name("Both")
                .intrinsicValue(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("10.00"))
                .marginOfSafety(new BigDecimal("9.00"))
                .intrinsicValueUpdatedAt(tenDaysAgo)
                .build();

        when(rankingRepository.findTop20BestRanked()).thenReturn(List.of(both));
        when(allocationGoalPort.findByUserId(userId)).thenReturn(List.of());
        when(portfolioSnapshotPort.getCurrentAllocationByUserId(userId)).thenReturn(Map.of());

        GenerationResult generationResult = generateGrahamRecommendationsUseCase.execute(userId);

        assertTrue(generationResult.recommendations().isEmpty());
        assertEquals(1, generationResult.excludedTickers().size());
        assertEquals("ABCD4", generationResult.excludedTickers().get(0).ticker());
        assertEquals("STALE_DATA", generationResult.excludedTickers().get(0).reason());
    }
}
