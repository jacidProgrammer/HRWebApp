package dev.jacid.hrApplication.domain.model;

import java.util.Objects;

/** Result of analysing the sentiment of a feedback message, e.g. {@code POSITIVE} with score {@code 0.98}. */
public record Sentiment(SentimentLabel label, double score) {

    public Sentiment {
        Objects.requireNonNull(label, "label");
    }
}
