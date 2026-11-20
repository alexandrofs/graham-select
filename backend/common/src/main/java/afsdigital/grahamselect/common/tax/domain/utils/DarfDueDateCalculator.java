package afsdigital.grahamselect.common.tax.domain.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.YearMonth;
import java.util.Set;

public final class DarfDueDateCalculator {

    private static final Set<MonthDay> FIXED_HOLIDAYS = Set.of(
            MonthDay.of(1, 1),   // Ano Novo
            MonthDay.of(4, 21),  // Tiradentes
            MonthDay.of(5, 1),   // Dia do Trabalho
            MonthDay.of(9, 7),   // Independência do Brasil
            MonthDay.of(10, 12), // Nossa Senhora Aparecida
            MonthDay.of(11, 2),  // Finados
            MonthDay.of(11, 15), // Proclamação da República
            MonthDay.of(11, 20), // Consciência Negra (Lei 14.759/2023)
            MonthDay.of(12, 25), // Natal
            MonthDay.of(12, 31)  // Último dia útil do ano sem expediente bancário (Resolução CMN/Bacen 4.880/2020)
    );

    private DarfDueDateCalculator() {
    }

    public static LocalDate calculateDueDate(YearMonth competence) {
        if (competence == null) {
            return null;
        }
        YearMonth nextMonth = competence.plusMonths(1);
        LocalDate date = nextMonth.atEndOfMonth();

        while (isWeekendOrHoliday(date)) {
            date = date.minusDays(1);
        }
        return date;
    }

    public static boolean isWeekendOrHoliday(LocalDate date) {
        if (date == null) {
            return false;
        }
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return true;
        }
        MonthDay md = MonthDay.of(date.getMonth(), date.getDayOfMonth());
        return FIXED_HOLIDAYS.contains(md);
    }
}
