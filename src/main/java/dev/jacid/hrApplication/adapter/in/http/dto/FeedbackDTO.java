package dev.jacid.hrApplication.adapter.in.http.dto;

/**
 * JSON representation of a feedback entry. {@code name} is the employee the feedback is about.
 * On requests only {@code name} and {@code message} are read; {@code score} and {@code label}
 * are filled in by the sentiment analysis.
 */
public record FeedbackDTO(String name, String message, Double score, String label) {

    public FeedbackDTO(String name, String message) {
        this(name, message, null, null);
    }
}
