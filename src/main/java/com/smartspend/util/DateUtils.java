package com.smartspend.util;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public final class DateUtils {

    private DateUtils() {}

    public static LocalDate getStartOfMonth(int month, int year) {
        return LocalDate.of(year, month, 1);
    }

    public static LocalDate getEndOfMonth(int month, int year) {
        YearMonth ym = YearMonth.of(year, month);
        return ym.atEndOfMonth();
    }

    public static String getMonthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    public static String getShortMonthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    public static int getDaysInMonth(int month, int year) {
        return YearMonth.of(year, month).lengthOfMonth();
    }
}
