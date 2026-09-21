package dev.jacid.hrApplication.domain.model;

/** Application-wide settings changed by managers at runtime (a single row in the database). */
public record AppSettings(boolean sentimentAnalysisEnabled) {

    public static AppSettings defaults() {
        return new AppSettings(true);
    }
}
