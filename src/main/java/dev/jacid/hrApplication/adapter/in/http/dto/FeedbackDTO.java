package dev.jacid.hrApplication.adapter.in.http.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * JSON representation of a feedback entry. {@code authorId}/{@code authorName} are {@code null} for
 * anonymous feedback (except for its author) and {@code sentiment} is {@code null} when not analysed.
 */
@Schema(description = "A piece of peer feedback")
public record FeedbackDTO(
        UUID id,
        UUID recipientId,
        @Schema(example = "Louisa Becker") String recipientName,
        @Schema(description = "Null for anonymous feedback (except for its author) and when the author was deleted", nullable = true) UUID authorId,
        @Schema(description = "Null for anonymous feedback (except for its author) and when the author was deleted", nullable = true) String authorName,
        boolean anonymous,
        @Schema(description = "Recognised company value", nullable = true,
                allowableValues = {"TEAMWORK", "OWNERSHIP", "CRAFT", "CUSTOMER_FOCUS", "GROWTH"}) String value,
        @Schema(example = "Great facilitation of the retro!") String message,
        @Schema(description = "Null when the analysis was disabled, not configured or failed", nullable = true) SentimentDTO sentiment,
        Instant createdAt) {

    @Schema(description = "Sentiment of the message as classified by the model")
    public record SentimentDTO(
            @Schema(allowableValues = {"POSITIVE", "NEUTRAL", "NEGATIVE"}) String label,
            @Schema(description = "Model confidence, 0..1", example = "0.97") double score) {}
}
