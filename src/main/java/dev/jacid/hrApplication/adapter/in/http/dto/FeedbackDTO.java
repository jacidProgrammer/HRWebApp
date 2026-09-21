package dev.jacid.hrApplication.adapter.in.http.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * JSON representation of a feedback entry. {@code authorId}/{@code authorName} are {@code null} for
 * anonymous feedback (except for its author) and {@code sentiment} is {@code null} when not analysed.
 */
public record FeedbackDTO(UUID id, UUID recipientId, String recipientName, UUID authorId, String authorName,
                          boolean anonymous, String value, String message, SentimentDTO sentiment, Instant createdAt) {

    public record SentimentDTO(String label, double score) {}
}
