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
        if (FIXED_HOLIDAYS.contains(md)) {
            return true;
        }
        return isMovableBankingHoliday(date);
    }

    /**
     * Checks Brazilian national/banking movable holidays derived from Easter Sunday
     * (Carnaval Monday/Tuesday, Good Friday, and Corpus Christi).
     */
    private static boolean isMovableBankingHoliday(LocalDate date) {
        LocalDate easter = calculateEasterSunday(date.getYear());
        return date.equals(easter.minusDays(48)) // Segunda-feira de Carnaval
                || date.equals(easter.minusDays(47)) // Terça-feira de Carnaval
                || date.equals(easter.minusDays(2))  // Sexta-feira Santa
                || date.equals(easter.plusDays(60)); // Corpus Christi
    }

    /**
     * Meeus/Jones/Butcher Gregorian algorithm for Easter Sunday.
     */
    static LocalDate calculateEasterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
