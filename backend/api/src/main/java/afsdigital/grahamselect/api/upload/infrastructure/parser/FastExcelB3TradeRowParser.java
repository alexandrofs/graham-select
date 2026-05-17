package afsdigital.grahamselect.api.upload.infrastructure.parser;

import afsdigital.grahamselect.common.upload.domain.model.B3TradeRow;
import afsdigital.grahamselect.common.upload.domain.model.B3TradeRowFailure;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Component
public class FastExcelB3TradeRowParser {

    private static final List<String> REQUIRED_HEADERS = List.of("ticker", "data", "quantidade", "preco");
    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void parse(
            InputStream inputStream,
            Consumer<B3TradeRow> successConsumer,
            Consumer<B3TradeRowFailure> failureConsumer
    ) throws IOException {
        try (ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
            Sheet sheet = workbook.getFirstSheet();
            try (Stream<Row> rows = sheet.openStream()) {
                var iterator = rows.iterator();
                if (!iterator.hasNext()) {
                    throw new IllegalArgumentException("Arquivo vazio.");
                }

                Row headerRow = iterator.next();
                Map<String, Integer> headerIndexes = extractHeaderIndexes(headerRow);
                validateRequiredHeaders(headerIndexes);

                while (iterator.hasNext()) {
                    Row row = iterator.next();
                    if (row.getFirstNonEmptyCell().isEmpty()) {
                        continue;
                    }

                    Map<String, String> rawPayload = extractRawPayload(row, headerIndexes);
                    try {
                        successConsumer.accept(toTradeRow(row, headerIndexes, rawPayload));
                    } catch (Exception exception) {
                        failureConsumer.accept(new B3TradeRowFailure(
                                row.getRowNum(),
                                rawPayload,
                                exception.getMessage() == null ? "Falha ao processar linha." : exception.getMessage()
                        ));
                    }
                }
            }
        }
    }

    private Map<String, Integer> extractHeaderIndexes(Row headerRow) {
        Map<String, Integer> indexes = new HashMap<>();
        headerRow.stream()
                .filter(Objects::nonNull)
                .forEach(cell -> indexes.put(normalize(cell.asString()), cell.getColumnIndex()));
        return indexes;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndexes) {
        if (!headerIndexes.keySet().containsAll(REQUIRED_HEADERS)) {
            throw new IllegalArgumentException("Arquivo inválido: Colunas obrigatórias não encontradas.");
        }
    }

    private Map<String, String> extractRawPayload(Row row, Map<String, Integer> headerIndexes) {
        Map<String, String> payload = new HashMap<>();
        headerIndexes.forEach((header, index) -> payload.put(header, row.getCellText(index)));
        return payload;
    }

    private B3TradeRow toTradeRow(Row row, Map<String, Integer> headerIndexes, Map<String, String> rawPayload) {
        Integer tickerIndex = headerIndexes.get("ticker");
        Integer dateIndex = headerIndexes.get("data");
        Integer quantityIndex = headerIndexes.get("quantidade");
        Integer priceIndex = headerIndexes.get("preco");
        Integer brokerIndex = headerIndexes.get("corretora");

        String ticker = requireText(rawPayload.get("ticker"), "Ticker é obrigatório").toUpperCase(Locale.ROOT);
        LocalDate tradeDate = parseTradeDate(row, dateIndex, rawPayload.get("data"));
        BigDecimal quantity = row.getCellAsNumber(quantityIndex)
                .orElseThrow(() -> new IllegalArgumentException("Quantidade inválida"));
        BigDecimal price = row.getCellAsNumber(priceIndex)
                .orElseThrow(() -> new IllegalArgumentException("Preço inválido"));
        String broker = brokerIndex == null ? null : emptyToNull(row.getCellText(brokerIndex));

        return new B3TradeRow(
                row.getRowNum(),
                ticker,
                tradeDate,
                quantity,
                price,
                broker
        );
    }

    private LocalDate parseTradeDate(Row row, Integer dateIndex, String rawValue) {
        if (dateIndex == null) {
            throw new IllegalArgumentException("Data é obrigatória");
        }

        LocalDateTime excelDate = row.getCellAsDate(dateIndex).orElse(null);
        if (excelDate != null) {
            return excelDate.toLocalDate();
        }

        String textValue = requireText(rawValue, "Data é obrigatória");
        return LocalDate.parse(textValue, BRAZILIAN_DATE);
    }

    private String requireText(String value, String message) {
        String normalized = emptyToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
