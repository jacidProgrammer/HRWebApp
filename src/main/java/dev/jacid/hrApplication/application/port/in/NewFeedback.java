package dev.jacid.hrApplication.application.port.in;

import java.util.UUID;

import dev.jacid.hrApplication.domain.model.FeedbackValue;

/** Feedback the caller wants to send. {@code value} is optional. */
public record NewFeedback(UUID recipientId, String message, FeedbackValue value, boolean anonymous) {
}
