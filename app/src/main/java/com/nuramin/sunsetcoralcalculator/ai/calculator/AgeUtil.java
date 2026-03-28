package com.nuramin.sunsetcoralcalculator.ai.calculator;

import androidx.annotation.NonNull;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Lightweight age calculation from numbers (day, month, year). Does not call existing app code.
 */
public final class AgeUtil {

    /**
     * Compute age in years, months, days from birth date.
     * todayY/M/D and birthY/M/D are 1-based month (Calendar style).
     */
    @NonNull
    public static String ageBetween(int birthYear, int birthMonth, int birthDay,
                                    int todayYear, int todayMonth, int todayDay) {
        int years = todayYear - birthYear;
        int months = todayMonth - birthMonth;
        int days = todayDay - birthDay;
        if (days < 0) {
            months--;
            Calendar c = Calendar.getInstance();
            c.set(todayYear, todayMonth - 1, 1);
            days += c.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
        if (months < 0) {
            years--;
            months += 12;
        }
        int d = Math.max(0, days);
        StringBuilder sb = new StringBuilder();
        if (years > 0) sb.append(years).append(" year").append(years != 1 ? "s" : "").append(" ");
        if (months > 0) sb.append(months).append(" month").append(months != 1 ? "s" : "").append(" ");
        sb.append(d).append(" day").append(d != 1 ? "s" : "");
        return sb.toString().trim();
    }

    @NonNull
    public static String formatAgeExplanation(int birthYear, int birthMonth, int birthDay) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, birthYear);
        c.set(Calendar.MONTH, Math.max(0, birthMonth - 1));
        c.set(Calendar.DAY_OF_MONTH, birthDay);
        return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(c.getTime());
    }
}
