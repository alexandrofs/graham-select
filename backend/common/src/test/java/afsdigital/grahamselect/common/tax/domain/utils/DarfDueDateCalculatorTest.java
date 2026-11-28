package afsdigital.grahamselect.common.tax.domain.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DarfDueDateCalculatorTest {

    @Test
    void shouldRecognizeMovableBrazilianBankingHolidays() {
        // 2025: Easter is 2025-04-20 -> Carnaval Mon 03-03, Tue 03-04; Good Friday 04-18; Corpus Christi 06-19
        assertTrue(DarfDueDateCalculator.isWeekendOrHoliday(LocalDate.of(2025, 3, 3)));
        assertTrue(DarfDueDateCalculator.isWeekendOrHoliday(LocalDate.of(2025, 3, 4)));
        assertTrue(DarfDueDateCalculator.isWeekendOrHoliday(LocalDate.of(2025, 4, 18)));
        assertTrue(DarfDueDateCalculator.isWeekendOrHoliday(LocalDate.of(2025, 6, 19)));

        // 2026: Easter is 2026-04-05 -> Carnaval Mon 02-16, Tue 02-17; Good Friday 04-03; Corpus Christi 06-04
        assertTrue(DarfDueDateCalculator.isWeekendOrHoliday(LocalDate.of(2026, 2, 16)));
        assertTrue(DarfDueDateCalculator.isWeekendOrHoliday(LocalDate.of(2026, 2, 17)));
        assertTrue(DarfDueDateCalculator.isWeekendOrHoliday(LocalDate.of(2026, 4, 3)));
        assertTrue(DarfDueDateCalculator.isWeekendOrHoliday(LocalDate.of(2026, 6, 4)));
    }

    @Test
    void shouldAnticipateDueDateWhenLastDayOfMonthIsMovableHoliday() {
        // March 2024: 2024-03-31 is Easter Sunday, 2024-03-30 is Saturday, 2024-03-29 is Good Friday!
        // Competence Feb 2024 -> due date in March 2024 must skip 31, 30, and 29 -> 2024-03-28 (Thursday)
        assertEquals(LocalDate.of(2024, 3, 28), DarfDueDateCalculator.calculateDueDate(YearMonth.of(2024, 2)));
    }
}
