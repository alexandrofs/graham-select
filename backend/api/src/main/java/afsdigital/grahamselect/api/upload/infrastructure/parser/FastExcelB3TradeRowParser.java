package afsdigital.grahamselect.api.upload.infrastructure.parser;

import afsdigital.grahamselect.common.upload.domain.model.B3TradeRow;
import afsdigital.grahamselect.common.upload.domain.model.B3TradeRowFailure;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
public class FastExcelB3TradeRowParser {

    @Getter
    @RequiredArgsConstructor
    private enum B3Header {
        TICKER("ticker", true, java.util.List.of("ticker", "codigo de negociacao", "codigo")),
        DATA("data", true, java.util.List.of("data", "data do negocio")),
        QUANTIDADE("quantidade", true, java.util.List.of("quantidade", "qtd")),
        PRECO("preco", true, java.util.List.of("preco", "preco unitario", "valor unitario")),
        CORRETORA("corretora", false, java.util.List.of("corretora", "instituicao"));

        private final String normalizedName;
        private final boolean required;
        private final java.util.List<String> aliases;

        public static B3Header fromNormalized(String value) {
            return Arrays.stream(values())
                    .filter(h -> h.aliases.contains(value) || h.normalizedName.equals(value))
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
                        log.error("Erro ao processar linha {} da planilha B3: {}", row.getRowNum() + 1, exception.getMessage());
                        log.debug("Stacktrace do erro da linha {}", row.getRowNum() + 1, exception);
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
        
        BigDecimal quantity = parseNumericCell(row, headerIndexes.get(B3Header.QUANTIDADE), "Quantidade inválida");
        BigDecimal price = parseNumericCell(row, headerIndexes.get(B3Header.PRECO), "Preço inválido");
        
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

    private BigDecimal parseNumericCell(Row row, Integer index, String errorMessage) {
        if (index == null) {
            throw new IllegalArgumentException(errorMessage);
        }

        try {
            // Tenta obter como número diretamente
            BigDecimal numericValue = row.getCellAsNumber(index).orElse(null);
            if (numericValue != null) {
                return numericValue;
            }
        } catch (Exception exception) {
            log.debug("Célula na coluna {} não é do tipo numérico nativo, aplicando fallback textual: {}", index, exception.getMessage());
        }

        // Se falhar ou for texto, faz fallback limpando a string
        String text = row.getCellText(index);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }

        try {
            // Remove R$, espaços normais e todos os tipos de brancos unicode (\u00A0, etc.)
            String cleaned = text.replace("R$", "")
                    .replace("\u00a0", "")
                    .replaceAll("\\s+", "")
                    .trim();

            if (cleaned.contains(",")) {
                // Formato PT-BR: remove pontos (milhar) e troca vírgula por ponto
                cleaned = cleaned.replace(".", "").replace(",", ".");
            } else if (cleaned.contains(".")) {
                // Formato sem vírgula, mas com ponto:
                // Para quantidade (inteiro), removemos todos os pontos (ex: 1.000 -> 1000)
                // Para preço, tratamos como milhar se tiver exatamente 3 dígitos após o ponto (ex: 1.200)
                if (errorMessage.toLowerCase().contains("quantidade")) {
                    cleaned = cleaned.replace(".", "");
                } else if (cleaned.matches(".*\\.\\d{3}")) {
                    cleaned = cleaned.replace(".", "");
                }
            }

            return new BigDecimal(cleaned);
        } catch (Exception e) {
            throw new IllegalArgumentException(errorMessage + ": " + text, e);
        }
    }

    private LocalDate parseTradeDate(Row row, Integer dateIndex, String rawValue) {
        if (dateIndex == null) {
            throw new IllegalArgumentException("Data é obrigatória");
        }

        try {
            LocalDateTime excelDate = row.getCellAsDate(dateIndex).orElse(null);
            if (excelDate != null) {
                return excelDate.toLocalDate();
            }
        } catch (Exception exception) {
            log.debug("Célula de data na coluna {} não é do tipo data nativo, aplicando fallback textual: {}", dateIndex, exception.getMessage());
        }

        String textValue = requireText(rawValue, "Data é obrigatória");
        // Remove espaços extras nas pontas e separa a string caso contenha hora embutida
        String datePart = textValue.trim().split("\\s+")[0];
        return LocalDate.parse(datePart, BRAZILIAN_DATE);
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
