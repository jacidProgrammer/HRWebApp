package dev.jacid.hrApplication.domain.model;

import java.util.Arrays;
import java.util.Locale;

import dev.jacid.hrApplication.domain.exception.InvalidFeedbackException;

/** Company values a piece of feedback can recognise. */
public enum FeedbackValue {
    TEAMWORK,
    OWNERSHIP,
    CRAFT,
    CUSTOMER_FOCUS,
    GROWTH;

    /** Parses a value name (case-insensitive); {@code null} or blank means "no value". */
    public static FeedbackValue parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return valueOf(text.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidFeedbackException("value must be one of " + Arrays.toString(values()));
        }
    }
}
