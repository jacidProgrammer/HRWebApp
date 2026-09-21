package dev.jacid.hrApplication.adapter.in.http;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import dev.jacid.hrApplication.domain.exception.InvalidRequestException;

/**
 * Parses the {@code from}/{@code to} query parameters. Both accept a date ({@code 2026-09-01}, a whole UTC day)
 * or a date-time with offset ({@code 2026-09-01T10:15:30Z}). Both bounds are inclusive.
 */
public final class QueryParameters {

    private QueryParameters() {
    }

    /** Start of the range (inclusive), or {@code null}. */
    public static Instant from(String text) {
        if (isBlank(text)) {
            return null;
        }
        String value = text.strip();
        return isDate(value) ? parseDate("from", value).atStartOfDay().toInstant(ZoneOffset.UTC) : parseDateTime("from", value);
    }

    /** End of the range as an exclusive bound: the day after a date, or just after a date-time. */
    public static Instant until(String text) {
        if (isBlank(text)) {
            return null;
        }
        String value = text.strip();
        return isDate(value)
                ? parseDate("to", value).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
                : parseDateTime("to", value).plus(1, ChronoUnit.MICROS);
    }

    private static boolean isDate(String value) {
        return value.length() == 10;
    }

    private static LocalDate parseDate(String name, String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid(name, value);
        }
    }

    private static Instant parseDateTime(String name, String value) {
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException e) {
            throw invalid(name, value);
        }
    }

    private static InvalidRequestException invalid(String name, String value) {
        return new InvalidRequestException("'" + name + "' must be an ISO-8601 date (2026-09-01) or date-time "
                + "(2026-09-01T10:15:30Z), got '" + value + "'");
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
