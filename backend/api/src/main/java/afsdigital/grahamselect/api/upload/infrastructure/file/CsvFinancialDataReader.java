package afsdigital.grahamselect.api.upload.infrastructure.file;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.upload.domain.repository.FinancialDataReader;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.NumberUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CsvFinancialDataReader implements FinancialDataReader {

    @Override
    public List<FinancialDataEvent> read(InputStream inputStream) {

        log.info("Starting file reading...");

        List<FinancialDataEvent> events = new ArrayList<>();

        try(CSVReader reader = configureCsvReader(inputStream)) {

            List<String[]> lines = reader.readAll();

            boolean isHeader = true;

            for (String[] line : lines) {

                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                FinancialDataEvent financialDataEvent = FinancialDataEvent.builder()
                        .resultDate(LocalDate.now())
                        .ticker(line[0])
                        .price(convertToDouble(line[1]))
                        .dividendYield(convertToDouble(line[2]))
                        .priceEarnings(convertToDouble(line[3]))
                        .priceToBook(convertToDouble(line[4]))
                        .priceToAssets(convertToDouble(line[5]))
                        .grossMargin(convertToDouble(line[6]))
                        .ebitMargin(convertToDouble(line[7]))
                        .netMargin(convertToDouble(line[8]))
                        .priceToEbit(convertToDouble(line[9]))
                        .evToEbit(convertToDouble(line[10]))
                        .netDebtToEbit(convertToDouble(line[11]))
                        .netDebtToEquity(convertToDouble(line[12]))
                        .priceToSales(convertToDouble(line[13]))
                        .priceToWorkingCapital(convertToDouble(line[14]))
                        .priceToCurrentAssets(convertToDouble(line[15]))
                        .currentLiquidity(convertToDouble(line[16]))
                        .roe(convertToDouble(line[17]))
                        .roa(convertToDouble(line[18]))
                        .roic(convertToDouble(line[19]))
                        .equityToAssets(convertToDouble(line[20]))
                        .liabilitiesToAssets(convertToDouble(line[21]))
                        .assetTurnover(convertToDouble(line[22]))
                        .revenueCAGR5Years(convertToDouble(line[23]))
                        .profitCAGR5Years(convertToDouble(line[24]))
                        .averageDailyLiquidity(convertToDouble(line[25]))
                        .bookValuePerShare(convertToDouble(line[26]))
                        .earningsPerShare(convertToDouble(line[27]))
                        .pegRatio(convertToDouble(line[28]))
                        .marketValue(convertToDouble(line[29]))
                        .build();

                events.add(financialDataEvent);

            }

        } catch (IOException | CsvException e) {
            log.error("Error reading CSV file", e);
            throw new RuntimeException("Failed to read CSV file", e);
        }
        log.info("{} lines was read", events.size());
        return events;
    }

    private static Double convertToDouble(String value) {
        return !StringUtils.hasLength(value) ? null : NumberUtils.parseNumber(value.replace(".", "").replace(',', '.'), Double.class);
    }

    private static CSVReader configureCsvReader(InputStream inputStream) {
        return new CSVReaderBuilder(new InputStreamReader(inputStream))
                .withCSVParser(new CSVParserBuilder()
                        .withSeparator(';')
                        .build()).build();
    }

}
