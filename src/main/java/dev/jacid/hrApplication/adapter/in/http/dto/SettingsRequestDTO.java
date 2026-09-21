package dev.jacid.hrApplication.adapter.in.http.dto;

/** Body of {@code PUT /settings}. */
public record SettingsRequestDTO(Boolean sentimentAnalysisEnabled) {}
