package dev.jacid.hrApplication.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Feedback written by {@code author} about {@code recipient}.
 * <ul>
 *   <li>The author is always stored (it enforces rules such as "no feedback to yourself"), but for
 *       {@code anonymous} feedback it is never shown to anybody except the author
 *       ({@link #withAuthorHidden()}). It is {@code null} when the author's record was deleted.</li>
 *   <li>{@code value} is the company value the feedback recognises; optional.</li>
 *   <li>{@code sentiment} is {@code null} when the message was not analysed.</li>
 * </ul>
 */
public record Feedback(UUID id,
                       Employee recipient,
                       Employee author,
                       boolean anonymous,
                       FeedbackValue value,
                       String message,
                       Sentiment sentiment,
                       Instant createdAt) {

    public static final int MAX_MESSAGE_LENGTH = 500;

    /** The feedback as seen by anybody but its author: anonymous feedback loses its author. */
    public Feedback withAuthorHidden() {
        return anonymous && author != null
                ? new Feedback(id, recipient, null, true, value, message, sentiment, createdAt)
                : this;
    }

    public boolean isAnalysed() {
        return sentiment != null;
    }

    public boolean hasLabel(SentimentLabel label) {
        return sentiment != null && sentiment.label() == label;
    }
}
