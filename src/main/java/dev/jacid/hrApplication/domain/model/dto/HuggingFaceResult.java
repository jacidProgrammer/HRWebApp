package dev.jacid.hrApplication.domain.model.dto;

public record HuggingFaceResult(
    String label,
    double score
) {}
