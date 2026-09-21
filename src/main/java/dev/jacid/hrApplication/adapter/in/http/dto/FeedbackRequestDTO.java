package dev.jacid.hrApplication.adapter.in.http.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/** Body of {@code POST /feedback}; {@code value} is optional and {@code anonymous} defaults to false. */
@Schema(description = "Feedback to send", requiredProperties = {"recipientId", "message"})
public record FeedbackRequestDTO(
        @Schema(description = "Id of the colleague the feedback is about (not the caller)") UUID recipientId,
        @Schema(description = "1 to 500 characters; surrounding whitespace is removed", minLength = 1, maxLength = 500,
                example = "Great facilitation of the retro!") String message,
        @Schema(description = "Optional company value the feedback recognises", nullable = true,
                allowableValues = {"TEAMWORK", "OWNERSHIP", "CRAFT", "CUSTOMER_FOCUS", "GROWTH"}) String value,
        @Schema(description = "Hide the author from everybody but the author; defaults to false", nullable = true) Boolean anonymous) {}
