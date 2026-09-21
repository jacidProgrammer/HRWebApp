package dev.jacid.hrApplication.domain.model;

import java.util.Locale;
import java.util.Optional;

public enum SentimentLabel {
    POSITIVE,
    NEUTRAL,
    NEGATIVE;

    /** Case-insensitive lookup, e.g. {@code "positive"}; empty for unknown labels. */
    public static Optional<SentimentLabel> fromName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(name.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
