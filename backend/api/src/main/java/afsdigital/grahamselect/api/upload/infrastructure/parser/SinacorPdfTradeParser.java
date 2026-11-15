package afsdigital.grahamselect.api.upload.infrastructure.parser;

import afsdigital.grahamselect.common.upload.domain.model.B3TradeRow;
import afsdigital.grahamselect.common.upload.domain.model.B3TradeRowFailure;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SinacorPdfTradeParser {

    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Pattern TRADE_LINE_START_PATTERN = Pattern.compile(
            "^(?:\\d+-)?(?:BOVESPA|BVMF)\\s+([CV])\\s+",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TRADE_VALUES_END_PATTERN = Pattern.compile(
            "(\\d+(?:[\\.,]\\d+)*)\\s+(\\d+(?:[\\.,]\\d+)*)\\s+(\\d+(?:[\\.,]\\d+)*)\\s+([DC])\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern TICKER_PATTERN = Pattern.compile(
            "\\b([A-Z0-9]{4}\\d{1,2}[F]?)\\b"
    );

    private static final Pattern FRACTIONAL_TICKER_PATTERN = Pattern.compile(
            "^([A-Z0-9]{4}\\d{1,2})F$"
    );

    private static final Pattern DATE_HEADER_PATTERN = Pattern.compile(
            "(?:Data\\s+preg[aã]o|Data\\s+do\\s+preg[aã]o|Data\\s+pregao)\\s*[:]?\\s*(\\d{2}/\\d{2}/\\d{4})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private static final Pattern BROKER_HEADER_PATTERN = Pattern.compile(
            "^(.*?)(?:CCTVM|DTVM|CORRETORA|INVESTIMENTOS|BANCO|S/A|LTDA)(.*?)$",
            Pattern.CASE_INSENSITIVE
    );

    public boolean isSinacorNote(InputStream is) {
        if (is == null) {
            return false;
        }
        try {
            byte[] bytes = is.readAllBytes();
            if (bytes.length < 10) {
                return false;
            }
            try (PDDocument doc = Loader.loadPDF(bytes)) {
                if (doc.getNumberOfPages() < 1) {
                    return false;
                }
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(1);
                stripper.setEndPage(Math.min(2, doc.getNumberOfPages()));
                String text = stripper.getText(doc);
                if (text == null || text.isBlank()) {
                    return false;
                }
                String upper = normalizeText(text);
                boolean hasTitle = upper.contains("NOTA DE CORRETAGEM") || upper.contains("NOTA DE NEGOCIACAO");
                boolean hasMarker = upper.contains("DATA PREGAO") || upper.contains("NR. NOTA")
                        || upper.contains("NUMERO DA NOTA") || upper.contains("FOLHA");
                return hasTitle && hasMarker;
            }
        } catch (Exception e) {
            log.error("Falha ao inspecionar PDF para marcador SINACOR: {}", e.getMessage(), e);
            return false;
        }
    }

    public void parse(
            InputStream inputStream,
            Consumer<B3TradeRow> successConsumer,
            Consumer<B3TradeRowFailure> failureConsumer
    ) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream não pode ser nulo.");
        }
        byte[] pdfBytes = inputStream.readAllBytes();
        if (pdfBytes.length == 0) {
            throw new IllegalArgumentException("Arquivo vazio.");
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.getNumberOfPages() < 1) {
                throw new IllegalArgumentException("Documento PDF sem páginas.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            long lineNumber = 0;
            LocalDate currentTradeDate = null;
            String currentBroker = null;

            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);
                if (pageText == null || pageText.isBlank()) {
                    continue;
                }

                LocalDate pageTradeDate = extractTradeDate(pageText);
                if (pageTradeDate != null) {
                    currentTradeDate = pageTradeDate;
                }
                String pageBroker = extractBroker(pageText);
                if (pageBroker != null) {
                    currentBroker = pageBroker;
                }

                String[] lines = pageText.split("\\r?\\n");
                for (String rawLine : lines) {
                    String line = rawLine.trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    if (isTradeLine(line)) {
                        lineNumber++;
                        try {
                            if (currentTradeDate == null) {
                                throw new IllegalArgumentException("Data do pregão não encontrada na nota SINACOR.");
                            }
                            B3TradeRow tradeRow = parseTradeLine(lineNumber, line, currentTradeDate, currentBroker);
                            successConsumer.accept(tradeRow);
                        } catch (Exception ex) {
                            log.error("Erro ao processar linha {} da nota SINACOR: {}", lineNumber, ex.getMessage(), ex);
                            failureConsumer.accept(new B3TradeRowFailure(
                                    lineNumber,
                                    Map.of(
                                            "rawLine", line,
                                            "broker", currentBroker != null ? currentBroker : "",
                                            "tradeDate", currentTradeDate != null ? currentTradeDate.toString() : ""
                                    ),
                                    ex.getMessage() == null ? "Falha ao processar linha da nota de corretagem." : ex.getMessage()
                            ));
                        }
                    }
                }
            }

            if (lineNumber == 0) {
                throw new IllegalArgumentException("Nenhuma operação de negociação encontrada na nota SINACOR.");
            }
        }
    }

    private boolean isTradeLine(String line) {
        return TRADE_LINE_START_PATTERN.matcher(line).find();
    }

    private B3TradeRow parseTradeLine(long lineNumber, String line, LocalDate defaultDate, String defaultBroker) {
        Matcher startMatcher = TRADE_LINE_START_PATTERN.matcher(line);
        if (!startMatcher.find()) {
            throw new IllegalArgumentException("Linha não inicia com o padrão de negociação SINACOR.");
        }

        String sideChar = startMatcher.group(1).toUpperCase(Locale.ROOT);
        String side = "C".equals(sideChar) ? "COMPRA" : "VENDA";

        Matcher endMatcher = TRADE_VALUES_END_PATTERN.matcher(line);
        if (!endMatcher.find()) {
            throw new IllegalArgumentException("Não foi possível extrair os valores numéricos da operação.");
        }

        String quantityStr = endMatcher.group(1);
        String priceStr = endMatcher.group(2);

        BigDecimal quantity = parseBigDecimal(quantityStr);
        BigDecimal price = parseBigDecimal(priceStr);

        // Substring between start and end values to find the ticker
        int startPos = startMatcher.end();
        int endPos = endMatcher.start();
        String middleSection = line.substring(startPos, endPos).trim();

        Matcher tickerMatcher = TICKER_PATTERN.matcher(middleSection);
        if (!tickerMatcher.find()) {
            throw new IllegalArgumentException("Ticker não encontrado na linha de negociação: " + middleSection);
        }

        String rawTicker = tickerMatcher.group(1).toUpperCase(Locale.ROOT);
        String ticker = normalizeTicker(rawTicker);

        if (defaultDate == null) {
            throw new IllegalArgumentException("Data do pregão não informada para a linha de negociação.");
        }
        String broker = defaultBroker != null && !defaultBroker.isBlank() ? defaultBroker : "CORRETORA";

        return new B3TradeRow(
                lineNumber,
                ticker,
                defaultDate,
                side,
                quantity,
                price,
                broker
        );
    }

    private String normalizeTicker(String rawTicker) {
        Matcher fractionalMatcher = FRACTIONAL_TICKER_PATTERN.matcher(rawTicker);
        if (fractionalMatcher.matches()) {
            return fractionalMatcher.group(1);
        }
        return rawTicker;
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Valor numérico vazio.");
        }
        String clean = value.trim();
        if (clean.contains(",") && clean.contains(".")) {
            clean = clean.replace(".", "").replace(",", ".");
        } else if (clean.contains(",")) {
            clean = clean.replace(",", ".");
        }
        return new BigDecimal(clean);
    }

    private LocalDate extractTradeDate(String text) {
        Matcher matcher = DATE_HEADER_PATTERN.matcher(text);
        if (matcher.find()) {
            String dateStr = matcher.group(1);
            try {
                return LocalDate.parse(dateStr, BRAZILIAN_DATE);
            } catch (Exception e) {
                log.error("Erro ao fazer parse da data do pregão {}: {}", dateStr, e.getMessage(), e);
            }
        }
        return null;
    }

    private String extractBroker(String text) {
        String[] lines = text.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.length() < 3) {
                continue;
            }
            String upper = line.toUpperCase(Locale.ROOT);
            if (upper.contains("NOTA DE") || upper.contains("FOLHA") || upper.contains("DATA PREGAO") || upper.contains("NR. NOTA") || isTradeLine(line)) {
                continue;
            }
            Matcher matcher = BROKER_HEADER_PATTERN.matcher(line);
            if (matcher.find()) {
                return line;
            }
        }
        return null;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }
}
