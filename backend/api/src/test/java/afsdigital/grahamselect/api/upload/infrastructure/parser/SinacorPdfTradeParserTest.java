package afsdigital.grahamselect.api.upload.infrastructure.parser;

import afsdigital.grahamselect.common.upload.domain.model.B3TradeRow;
import afsdigital.grahamselect.common.upload.domain.model.B3TradeRowFailure;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SinacorPdfTradeParserTest {

    private SinacorPdfTradeParser parser;

    @BeforeEach
    void setUp() {
        parser = new SinacorPdfTradeParser();
    }

    private byte[] createPdfWithLines(List<String> lines) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.newLineAtOffset(40, 750);
                for (String line : lines) {
                    cs.showText(line);
                    cs.newLineAtOffset(0, -15);
                }
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private byte[] createMultiPagePdf(List<String> page1Lines, List<String> page2Lines) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page1 = new PDPage();
            doc.addPage(page1);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page1)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.newLineAtOffset(40, 750);
                for (String line : page1Lines) {
                    cs.showText(line);
                    cs.newLineAtOffset(0, -15);
                }
                cs.endText();
            }

            PDPage page2 = new PDPage();
            doc.addPage(page2);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page2)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.newLineAtOffset(40, 750);
                for (String line : page2Lines) {
                    cs.showText(line);
                    cs.newLineAtOffset(0, -15);
                }
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("isSinacorNote deve retornar true para nota válida com marcadores SINACOR")
    void isSinacorNote_ValidNote_ReturnsTrue() throws IOException {
        byte[] pdf = createPdfWithLines(List.of(
                "NOTA DE CORRETAGEM",
                "XP INVESTIMENTOS CCTVM S/A",
                "Nr. nota: 998877 Data pregao: 15/05/2026"
        ));

        assertThat(parser.isSinacorNote(new ByteArrayInputStream(pdf))).isTrue();
    }

    @Test
    @DisplayName("isSinacorNote deve retornar false para PDF sem marcadores SINACOR")
    void isSinacorNote_InvalidNote_ReturnsFalse() throws IOException {
        byte[] pdf = createPdfWithLines(List.of(
                "EXTRATO CONSOLIDADO",
                "BANCO DO BRASIL",
                "Saldo em conta corrente"
        ));

        assertThat(parser.isSinacorNote(new ByteArrayInputStream(pdf))).isFalse();
    }

    @Test
    @DisplayName("isSinacorNote deve retornar false para stream vazio ou corrompido")
    void isSinacorNote_EmptyOrCorruptedStream_ReturnsFalse() {
        assertThat(parser.isSinacorNote(new ByteArrayInputStream(new byte[0]))).isFalse();
        assertThat(parser.isSinacorNote(new ByteArrayInputStream(new byte[]{1, 2, 3}))).isFalse();
    }

    @Test
    @DisplayName("parse deve extrair operações de compra, venda e fracionário com normalização de tickers e valores")
    void parse_ValidSinacorNote_ExtractsTradesCorrectly() throws IOException {
        byte[] pdf = createPdfWithLines(List.of(
                "NOTA DE CORRETAGEM",
                "XP INVESTIMENTOS CCTVM S/A",
                "Nr. nota: 12345 Data pregão: 10/05/2026 Folha: 1",
                "Q Negociação C/V Tipo mercado Prazo Especificação do título Obs. (*) Quantidade Preço / Ajuste Valor Operação / Ajuste D/C",
                "1-BOVESPA C VISTA PETR4 PETROBRAS PN 100 38,50 3.850,00 D",
                "1-BOVESPA V VISTA VALE3 VALE ON 50 62,10 3.105,00 C",
                "1-BOVESPA C FRACIONARIO PETR4F PETROBRAS PN 15 38,50 577,50 D",
                "1-BOVESPA C VISTA HGLG11 CSHG LOGISTICA FII 10 165,80 1.658,00 D"
        ));

        List<B3TradeRow> successes = new ArrayList<>();
        List<B3TradeRowFailure> failures = new ArrayList<>();

        parser.parse(new ByteArrayInputStream(pdf), successes::add, failures::add);

        assertThat(failures).isEmpty();
        assertThat(successes).hasSize(4);

        // Trade 1: Compra PETR4
        B3TradeRow t1 = successes.get(0);
        assertThat(t1.ticker()).isEqualTo("PETR4");
        assertThat(t1.side()).isEqualTo("COMPRA");
        assertThat(t1.tradeDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(t1.quantity()).isEqualByComparingTo("100");
        assertThat(t1.price()).isEqualByComparingTo("38.50");
        assertThat(t1.broker()).contains("XP INVESTIMENTOS");

        // Trade 2: Venda VALE3
        B3TradeRow t2 = successes.get(1);
        assertThat(t2.ticker()).isEqualTo("VALE3");
        assertThat(t2.side()).isEqualTo("VENDA");
        assertThat(t2.tradeDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(t2.quantity()).isEqualByComparingTo("50");
        assertThat(t2.price()).isEqualByComparingTo("62.10");

        // Trade 3: Compra Fracionário PETR4F normalizado para PETR4
        B3TradeRow t3 = successes.get(2);
        assertThat(t3.ticker()).isEqualTo("PETR4");
        assertThat(t3.side()).isEqualTo("COMPRA");
        assertThat(t3.quantity()).isEqualByComparingTo("15");
        assertThat(t3.price()).isEqualByComparingTo("38.50");

        // Trade 4: FII HGLG11
        B3TradeRow t4 = successes.get(3);
        assertThat(t4.ticker()).isEqualTo("HGLG11");
        assertThat(t4.side()).isEqualTo("COMPRA");
        assertThat(t4.quantity()).isEqualByComparingTo("10");
        assertThat(t4.price()).isEqualByComparingTo("165.80");
    }

    @Test
    @DisplayName("parse deve suportar notas multipáginas consolidando todas as operações")
    void parse_MultiPageSinacorNote_ExtractsAllTrades() throws IOException {
        List<String> page1 = List.of(
                "NOTA DE NEGOCIAÇÃO",
                "CLEAR CORRETORA",
                "Nr. nota: 44556 Data pregao: 12/06/2026 Folha: 1/2",
                "1-BOVESPA C VISTA ITUB4 ITAU UNIBANCO PN 200 32,40 6.480,00 D",
                "1-BOVESPA C VISTA BBDC4 BRADESCO PN 100 14,20 1.420,00 D"
        );

        List<String> page2 = List.of(
                "NOTA DE NEGOCIAÇÃO",
                "CLEAR CORRETORA",
                "Nr. nota: 44556 Data pregao: 12/06/2026 Folha: 2/2",
                "1-BOVESPA V VISTA WEGE3 WEG ON 50 45,00 2.250,00 C",
                "1-BOVESPA C FRACIONARIO KLBN11F KLABIN UNT 8 22,50 180,00 D"
        );

        byte[] pdf = createMultiPagePdf(page1, page2);

        List<B3TradeRow> successes = new ArrayList<>();
        List<B3TradeRowFailure> failures = new ArrayList<>();

        parser.parse(new ByteArrayInputStream(pdf), successes::add, failures::add);

        assertThat(failures).isEmpty();
        assertThat(successes).hasSize(4);
        assertThat(successes.get(0).ticker()).isEqualTo("ITUB4");
        assertThat(successes.get(1).ticker()).isEqualTo("BBDC4");
        assertThat(successes.get(2).ticker()).isEqualTo("WEGE3");
        assertThat(successes.get(3).ticker()).isEqualTo("KLBN11"); // fracionário normalizado
        assertThat(successes.get(3).broker()).contains("CLEAR CORRETORA");
    }

    @Test
    @DisplayName("parse deve reportar falha em linha corrompida e continuar processando linhas válidas")
    void parse_CorruptedLine_ReportsFailureAndContinues() throws IOException {
        byte[] pdf = createPdfWithLines(List.of(
                "NOTA DE CORRETAGEM",
                "INTER DTVM LTDA",
                "Data pregão: 20/07/2026 Nr. nota: 7788",
                "1-BOVESPA C VISTA PETR4 PETROBRAS PN 100 38,50 3.850,00 D",
                "1-BOVESPA C VISTA INVALID_LINE_MISSING_NUMBERS D",
                "1-BOVESPA V VISTA VALE3 VALE ON 50 62,10 3.105,00 C"
        ));

        List<B3TradeRow> successes = new ArrayList<>();
        List<B3TradeRowFailure> failures = new ArrayList<>();

        parser.parse(new ByteArrayInputStream(pdf), successes::add, failures::add);

        assertThat(successes).hasSize(2);
        assertThat(successes.get(0).ticker()).isEqualTo("PETR4");
        assertThat(successes.get(1).ticker()).isEqualTo("VALE3");
    }

    @Test
    @DisplayName("parse deve suportar ticker B3SA3, fracionário B3SA3F e prefixos de câmara variados como 2-BOVESPA")
    void parse_B3SA3AndCustomClearingPrefix_ExtractsSuccessfully() throws IOException {
        byte[] pdf = createPdfWithLines(List.of(
                "NOTA DE CORRETAGEM",
                "BTG PACTUAL CTVM S.A.",
                "Nr. nota: 556677 Data pregão: 25/08/2026 Folha: 1",
                "2-BOVESPA C VISTA B3SA3 B3 ON 100 12,80 1.280,00 D",
                "BOVESPA V FRACIONARIO B3SA3F B3 ON 15 12,80 192,00 C"
        ));

        List<B3TradeRow> successes = new ArrayList<>();
        List<B3TradeRowFailure> failures = new ArrayList<>();

        parser.parse(new ByteArrayInputStream(pdf), successes::add, failures::add);

        assertThat(failures).isEmpty();
        assertThat(successes).hasSize(2);
        assertThat(successes.get(0).ticker()).isEqualTo("B3SA3");
        assertThat(successes.get(0).side()).isEqualTo("COMPRA");
        assertThat(successes.get(0).tradeDate()).isEqualTo(LocalDate.of(2026, 8, 25));
        assertThat(successes.get(1).ticker()).isEqualTo("B3SA3");
        assertThat(successes.get(1).side()).isEqualTo("VENDA");
    }

    @Test
    @DisplayName("parse deve suportar páginas com pregões diferentes em um mesmo documento multipáginas")
    void parse_MultiPageMultiPregao_ExtractsEachPageWithItsOwnDate() throws IOException {
        List<String> page1 = List.of(
                "NOTA DE CORRETAGEM",
                "XP INVESTIMENTOS CCTVM S/A",
                "Nr. nota: 101 Data pregão: 10/05/2026 Folha: 1",
                "1-BOVESPA C VISTA PETR4 PETROBRAS PN 100 38,50 3.850,00 D"
        );

        List<String> page2 = List.of(
                "NOTA DE CORRETAGEM",
                "XP INVESTIMENTOS CCTVM S/A",
                "Nr. nota: 102 Data pregão: 15/05/2026 Folha: 1",
                "1-BOVESPA V VISTA VALE3 VALE ON 50 62,10 3.105,00 C"
        );

        byte[] pdf = createMultiPagePdf(page1, page2);

        List<B3TradeRow> successes = new ArrayList<>();
        List<B3TradeRowFailure> failures = new ArrayList<>();

        parser.parse(new ByteArrayInputStream(pdf), successes::add, failures::add);

        assertThat(failures).isEmpty();
        assertThat(successes).hasSize(2);
        assertThat(successes.get(0).tradeDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(successes.get(1).tradeDate()).isEqualTo(LocalDate.of(2026, 5, 15));
    }

    @Test
    @DisplayName("parse deve lançar exceção se nenhuma operação for encontrada na nota")
    void parse_ZeroTrades_ThrowsIllegalArgumentException() throws IOException {
        byte[] pdf = createPdfWithLines(List.of(
                "NOTA DE CORRETAGEM",
                "XP INVESTIMENTOS CCTVM S/A",
                "Nr. nota: 123 Data pregão: 10/05/2026 Folha: 1",
                "Nenhuma operação realizada nesta data"
        ));

        List<B3TradeRow> successes = new ArrayList<>();
        List<B3TradeRowFailure> failures = new ArrayList<>();

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(new ByteArrayInputStream(pdf), successes::add, failures::add)
        );
    }
}
