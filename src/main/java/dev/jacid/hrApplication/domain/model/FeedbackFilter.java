package dev.jacid.hrApplication.domain.model;

import java.time.Instant;
import java.util.UUID;

import dev.jacid.hrApplication.domain.exception.InvalidRequestException;

/**
 * Criteria for searching feedback; every {@code null} criterion matches everything.
 *
 * @param recipientId only feedback about this employee
 * @param department  only feedback about employees of this department (case-insensitive)
 * @param from        created at or after this instant (inclusive)
 * @param until       created before this instant (exclusive)
 * @param sentiment   only feedback with this sentiment, or without one ({@link SentimentFilter#NONE})
 */
public record FeedbackFilter(UUID recipientId, String department, Instant from, Instant until, SentimentFilter sentiment) {

    public FeedbackFilter {
        department = department == null || department.isBlank() ? null : department.strip();
        if (from != null && until != null && !from.isBefore(until)) {
            throw new InvalidRequestException("'from' must be before 'to'");
        }
    }

    public static FeedbackFilter none() {
        return new FeedbackFilter(null, null, null, null, null);
    }
}
