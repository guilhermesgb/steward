package com.github.guilhermesgb.steward.utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DateUtils {

    public static String formatDate(DateTime date) {
        return formatDate(date, "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    }

    private static String formatDate(DateTime date, String pattern) {
        return date == null ? null : new SimpleDateFormat(pattern,
            Locale.US).format(date.toDateTime(DateTimeZone.UTC).toDate());
    }

}
