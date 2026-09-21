package dev.jacid.hrApplication.adapter.in.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Body of {@code PUT /settings}. */
@Schema(description = "New settings", requiredProperties = "sentimentAnalysisEnabled")
public record SettingsRequestDTO(Boolean sentimentAnalysisEnabled) {}
