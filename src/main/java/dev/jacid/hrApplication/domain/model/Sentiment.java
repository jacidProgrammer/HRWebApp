package dev.jacid.hrApplication.domain.model;

/** Result of analysing the sentiment of a feedback message, e.g. {@code positive} with score {@code 0.98}. */
public record Sentiment(String label, Double score) {
}
