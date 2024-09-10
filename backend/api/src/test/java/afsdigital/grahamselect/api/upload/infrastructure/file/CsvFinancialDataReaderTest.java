package afsdigital.grahamselect.api.upload.infrastructure.file;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import com.opencsv.exceptions.CsvException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CsvFinancialDataReaderTest {

    private CsvFinancialDataReader csvFinancialDataReader;

    @BeforeEach
    void setUp() {
        csvFinancialDataReader = new CsvFinancialDataReader();
    }

    @Test
    void read_WhenCsvIsValid_ShouldParseEvents() throws IOException, CsvException {

        InputStream inputStream = new ClassPathResource("test-financial-data.csv").getInputStream();

        List<FinancialDataEvent> events = csvFinancialDataReader.read(inputStream);

        assertEquals(2, events.size()); // assuming there are 2 valid records in test-financial-data.csv

        FinancialDataEvent event1 = events.get(0);
        assertEquals("AALR3", event1.getTicker());
        assertEquals(15.30, event1.getPrice());
        assertEquals(null, event1.getDividendYield());
        assertEquals(-8.29, event1.getPriceEarnings());
        assertEquals(1.62, event1.getPriceToBook());
        assertEquals(0.67, event1.getPriceToAssets());
        assertEquals(29.80, event1.getGrossMargin());
        assertEquals(3.29, event1.getEbitMargin());
        assertEquals(-18.37, event1.getNetMargin());
        assertEquals(46.32, event1.getPriceToEbit());
        assertEquals(61.33, event1.getEvToEbit());
        assertEquals(15.01, event1.getNetDebtToEbit());
        assertEquals(0.52, event1.getNetDebtToEquity());
        assertEquals(1.52, event1.getPriceToSales());
        assertEquals(-11.64, event1.getPriceToWorkingCapital());
        assertEquals(-0.85, event1.getPriceToCurrentAssets());
        assertEquals(0.78, event1.getCurrentLiquidity());
        assertEquals(-19.51, event1.getRoe());
        assertEquals(-8.11, event1.getRoa());
        assertEquals(1.34, event1.getRoic());
        assertEquals(0.42, event1.getEquityToAssets());
        assertEquals(0.58, event1.getLiabilitiesToAssets());
        assertEquals(0.44, event1.getAssetTurnover());
        assertEquals(1.84, event1.getRevenueCAGR5Years());
        assertEquals(null, event1.getProfitCAGR5Years());
        assertEquals(681118.58, event1.getAverageDailyLiquidity());
        assertEquals(9.47, event1.getBookValuePerShare());
        assertEquals(-1.85, event1.getEarningsPerShare());
        assertEquals(0.35, event1.getPegRatio());
        assertEquals(1809880084.80, event1.getMarketValue());

        FinancialDataEvent event2 = events.get(1);
        assertEquals("ABCB4", event2.getTicker());
        assertEquals(23.01, event2.getPrice());
        assertEquals(6.63, event2.getDividendYield());
        assertEquals(6.38, event2.getPriceEarnings());
        assertEquals(0.96, event2.getPriceToBook());
        assertEquals(0.09, event2.getPriceToAssets());
        assertEquals(16.03, event2.getGrossMargin());
        assertEquals(9.38, event2.getEbitMargin());
        assertEquals(10.00, event2.getNetMargin());
        assertEquals(6.80, event2.getPriceToEbit());
        assertEquals(63.38, event2.getEvToEbit());
        assertEquals(56.55, event2.getNetDebtToEbit());
        assertEquals(7.95, event2.getNetDebtToEquity());
        assertEquals(0.64, event2.getPriceToSales());
        assertEquals(null, event2.getPriceToWorkingCapital());
        assertEquals(-0.09, event2.getPriceToCurrentAssets());
        assertEquals(null, event2.getCurrentLiquidity());
        assertEquals(14.98, event2.getRoe());
        assertEquals(1.46, event2.getRoa());
        assertEquals(1.43, event2.getRoic());
        assertEquals(0.10, event2.getEquityToAssets());
        assertEquals(0.90, event2.getLiabilitiesToAssets());
        assertEquals(0.15, event2.getAssetTurnover());
        assertEquals(20.08, event2.getRevenueCAGR5Years());
        assertEquals(16.19, event2.getProfitCAGR5Years());
        assertEquals(19843344.58, event2.getAverageDailyLiquidity());
        assertEquals(24.07, event2.getBookValuePerShare());
        assertEquals(3.61, event2.getEarningsPerShare());
        assertEquals(-0.21, event2.getPegRatio());
        assertEquals(5651573396.70, event2.getMarketValue());

    }

    @Test
    void read_WhenCsvIsEmpty_ShouldReturnEmptyList() throws IOException, CsvException {

        InputStream inputStream = new ClassPathResource("empty-csv.csv").getInputStream();
        List<FinancialDataEvent> events = csvFinancialDataReader.read(inputStream);
        assertEquals(0, events.size());

    }

    @Test
    void read_WhenCsvIsMalformed_ShouldThrowException() throws IOException {

        InputStream inputStream = new ClassPathResource("malformed-csv.csv").getInputStream();
        assertThrows(RuntimeException.class, () -> csvFinancialDataReader.read(inputStream));

    }
}
