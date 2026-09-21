package dev.jacid.hrApplication.domain.model;

/**
 * Feedback written by {@code reporter} about {@code employee}.
 * {@code sentiment} is {@code null} when the message could not be analysed.
 */
public record Feedback(Long id,
                       Employee employee,
                       Employee reporter,
                       String message,
                       Sentiment sentiment) {
}
