package dev.jacid.hrApplication.domain.model.dto;

public record FeedbackDTO(String name, String message, Double score, String label) {

    public FeedbackDTO(String name, String message) {
        this(name, message, null, null);
    }
}