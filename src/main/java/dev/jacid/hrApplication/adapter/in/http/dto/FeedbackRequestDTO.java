package dev.jacid.hrApplication.adapter.in.http.dto;

import java.util.UUID;

/** Body of {@code POST /feedback}; {@code value} is optional and {@code anonymous} defaults to false. */
public record FeedbackRequestDTO(UUID recipientId, String message, String value, Boolean anonymous) {}
