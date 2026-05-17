package afsdigital.grahamselect.api.upload.infrastructure.parser;

import afsdigital.grahamselect.common.upload.domain.model.B3TradeRow;
import afsdigital.grahamselect.common.upload.domain.model.B3TradeRowFailure;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Component
public class FastExcelB3TradeRowParser {

    @Getter
    @RequiredArgsConstructor
    private enum B3Header {
        TICKER("ticker", true),
        DATA("data", true),
        QUANTIDADE("quantidade", true),
        PRECO("preco", true),
        CORRETORA("corretora", false);

        private final String normalizedName;
        private final boolean required;

        public static B3Header fromNormalized(String value) {
            return Arrays.stream(values())
                    .filter(h -> h.normalizedName.equals(value))
                    .findFirst()
                    .orElse(null);
        }
    }

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
                Map<B3Header, Integer> headerIndexes = extractHeaderIndexes(headerRow);
                validateRequiredHeaders(headerIndexes);

                while (iterator.hasNext()) {
                    Row row = iterator.next();
                    if (row.getFirstNonEmptyCell().isEmpty()) {
                        continue;
                    }

                    Map<B3Header, String> rawPayload = extractRawPayload(row, headerIndexes);
                    try {
                        successConsumer.accept(toTradeRow(row, headerIndexes, rawPayload));
                    } catch (Exception exception) {
                        failureConsumer.accept(new B3TradeRowFailure(
                                row.getRowNum(),
                                convertPayloadToStringMap(rawPayload),
                                exception.getMessage() == null ? "Falha ao processar linha." : exception.getMessage()
                        ));
                    }
                }
            }
        }
    }

    private Map<B3Header, Integer> extractHeaderIndexes(Row headerRow) {
        Map<B3Header, Integer> indexes = new HashMap<>();
        headerRow.stream()
                .filter(Objects::nonNull)
                .forEach(cell -> {
                    B3Header header = B3Header.fromNormalized(normalize(cell.asString()));
                    if (header != null) {
                        indexes.put(header, cell.getColumnIndex());
                    }
                });
        return indexes;
    }

    private void validateRequiredHeaders(Map<B3Header, Integer> headerIndexes) {
        for (B3Header header : B3Header.values()) {
            if (header.isRequired() && !headerIndexes.containsKey(header)) {
                throw new IllegalArgumentException("Arquivo inválido: Coluna obrigatória '" + header.normalizedName + "' não encontrada.");
            }
        }
    }

    private Map<B3Header, String> extractRawPayload(Row row, Map<B3Header, Integer> headerIndexes) {
        Map<B3Header, String> payload = new HashMap<>();
        headerIndexes.forEach((header, index) -> payload.put(header, row.getCellText(index)));
        return payload;
    }

    private Map<String, String> convertPayloadToStringMap(Map<B3Header, String> payload) {
        Map<String, String> stringMap = new HashMap<>();
        payload.forEach((k, v) -> stringMap.put(k.getNormalizedName(), v));
        return stringMap;
    }

    private B3TradeRow toTradeRow(Row row, Map<B3Header, Integer> headerIndexes, Map<B3Header, String> rawPayload) {
        String ticker = requireText(rawPayload.get(B3Header.TICKER), "Ticker é obrigatório").toUpperCase(Locale.ROOT);
        LocalDate tradeDate = parseTradeDate(row, headerIndexes.get(B3Header.DATA), rawPayload.get(B3Header.DATA));
        
        BigDecimal quantity = row.getCellAsNumber(headerIndexes.get(B3Header.QUANTIDADE))
                .orElseThrow(() -> new IllegalArgumentException("Quantidade inválida"));
        BigDecimal price = row.getCellAsNumber(headerIndexes.get(B3Header.PRECO))
                .orElseThrow(() -> new IllegalArgumentException("Preço inválido"));
        
        String broker = null;
        if (headerIndexes.containsKey(B3Header.CORRETORA)) {
            broker = emptyToNull(row.getCellText(headerIndexes.get(B3Header.CORRETORA)));
        }

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
