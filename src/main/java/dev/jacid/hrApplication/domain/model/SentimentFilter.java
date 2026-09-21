package dev.jacid.hrApplication.domain.model;

import java.util.Arrays;
import java.util.Locale;

import dev.jacid.hrApplication.domain.exception.InvalidRequestException;

/** Sentiment criterion when searching feedback: one label, or {@code NONE} for feedback without a sentiment. */
public enum SentimentFilter {
    POSITIVE,
    NEUTRAL,
    NEGATIVE,
    NONE;

    /** The label to match, or {@code null} for {@link #NONE}. */
    public SentimentLabel label() {
        return this == NONE ? null : SentimentLabel.valueOf(name());
    }

    public static SentimentFilter parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return valueOf(text.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("sentiment must be one of " + Arrays.toString(values()));
        }
    }
}
