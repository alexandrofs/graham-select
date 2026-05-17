package afsdigital.grahamselect.api.upload.infrastructure.parser;

import afsdigital.grahamselect.common.upload.domain.model.B3TradeRow;
import afsdigital.grahamselect.common.upload.domain.model.B3TradeRowFailure;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.junit.jupiter.api.BeforeEach;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FastExcelB3TradeRowParserTest {

    private FastExcelB3TradeRowParser parser;

    @BeforeEach
    void setUp() {
        parser = new FastExcelB3TradeRowParser();
    }

    @Test
    void parse_WhenFileIsValid_ShouldCallSuccessConsumer() throws IOException {
        // Given
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (Workbook wb = new Workbook(bos, "Test", "1.0")) {
            Worksheet ws = wb.newWorksheet("Sheet1");
            ws.value(0, 0, "ticker");
            ws.value(0, 1, "data");
            ws.value(0, 2, "quantidade");
            ws.value(0, 3, "preco");
            ws.value(0, 4, "corretora");

            ws.value(1, 0, "PETR4");
            ws.value(1, 1, LocalDate.of(2026, 5, 10)); // Date as Date
            ws.value(1, 2, new BigDecimal("100"));      // Quantity as Number
            ws.value(1, 3, new BigDecimal("35.50"));    // Price as Number
            ws.value(1, 4, "XP");
        }
        
        InputStream inputStream = new ByteArrayInputStream(bos.toByteArray());
        List<B3TradeRow> results = new ArrayList<>();

        // When
        parser.parse(inputStream, results::add, failure -> {});

        // Then
        assertThat(results).hasSize(1);
        B3TradeRow row = results.get(0);
        assertThat(row.ticker()).isEqualTo("PETR4");
        assertThat(row.tradeDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(row.quantity()).isEqualByComparingTo("100");
        assertThat(row.price()).isEqualByComparingTo("35.50");
        assertThat(row.broker()).isEqualTo("XP");
    }

    @Test
    void parse_WhenRowIsCorrupted_ShouldCallFailureConsumer() throws IOException {
        // Given
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (Workbook wb = new Workbook(bos, "Test", "1.0")) {
            Worksheet ws = wb.newWorksheet("Sheet1");
            ws.value(0, 0, "ticker");
            ws.value(0, 1, "data");
            ws.value(0, 2, "quantidade");
            ws.value(0, 3, "preco");

            ws.value(1, 0, "VALE3");
            ws.value(1, 1, "invalid-date"); // Invalid type for date
            ws.value(1, 2, "abc");          // Invalid type for quantity
            ws.value(1, 3, new BigDecimal("50.00"));
        }

        InputStream inputStream = new ByteArrayInputStream(bos.toByteArray());
        List<B3TradeRowFailure> failures = new ArrayList<>();

        // When
        parser.parse(inputStream, success -> {}, failures::add);

        // Then
        assertThat(failures).hasSize(1);
        B3TradeRowFailure failure = failures.get(0);
        assertThat(failure.lineNumber()).isEqualTo(2); // FastExcel row numbering is 0-based, row index 1 is physical row 2
        assertThat(failure.reason()).containsIgnoringCase("Wrong cell type");
    }

    @Test
    void parse_WhenRequiredHeaderIsMissing_ShouldThrowException() throws IOException {
        // Given
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (Workbook wb = new Workbook(bos, "Test", "1.0")) {
            Worksheet ws = wb.newWorksheet("Sheet1");
            ws.value(0, 0, "ticker");
            ws.value(0, 1, "quantidade");
            ws.value(0, 2, "preco");
        }

        InputStream inputStream = new ByteArrayInputStream(bos.toByteArray());

        // When & Then
        assertThatThrownBy(() -> parser.parse(inputStream, success -> {}, failure -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data");
    }

    @Test
    void parse_WhenFileIsEmpty_ShouldThrowException() throws IOException {
        // Given
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (Workbook wb = new Workbook(bos, "Test", "1.0")) {
            wb.newWorksheet("Sheet1");
        }
        InputStream inputStream = new ByteArrayInputStream(bos.toByteArray());

        // When & Then
        assertThatThrownBy(() -> parser.parse(inputStream, success -> {}, failure -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vazio");
    }
}
