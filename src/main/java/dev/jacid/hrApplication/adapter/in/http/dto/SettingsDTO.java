package dev.jacid.hrApplication.adapter.in.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Runtime settings")
public record SettingsDTO(
        @Schema(description = "Managers want new feedback to be analysed (stored in the database)") boolean sentimentAnalysisEnabled,
        @Schema(description = "The analysis is configured (a Hugging Face token is set); if false nothing is analysed") boolean sentimentAnalysisAvailable) {}
