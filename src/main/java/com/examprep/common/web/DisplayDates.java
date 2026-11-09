package com.examprep.common.web;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formats dates for server-rendered pages without adding a template date library.
 */
public final class DisplayDates {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH).withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_LONG =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    private DisplayDates() {
    }

    public static String dateTime(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(instant);
    }

    public static String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    public static String longDate(LocalDate date) {
        return date == null ? "" : DATE_LONG.format(date);
    }

    public static String duration(int totalSeconds) {
        int seconds = Math.max(0, totalSeconds);
        int minutes = seconds / 60;
        int remainder = seconds % 60;
        if (minutes >= 60) {
            int hours = minutes / 60;
            return hours + " h " + (minutes % 60) + " min";
        }
        return minutes + " min " + (remainder < 10 ? "0" + remainder : remainder) + " s";
    }
}
