package dev.jacid.hrApplication.adapter.in.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Body of every error response produced by the API (not by Spring Security). */
@Schema(description = "Error returned by the API")
public record ErrorResponse(
        @Schema(description = "HTTP status name", example = "BAD_REQUEST") String code,
        @Schema(description = "Human-readable explanation", example = "message must contain 1 to 500 characters") String message) {}
