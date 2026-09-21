package dev.jacid.hrApplication.application.port.in;

public interface SettingsUseCases {
    SettingsStatus getSettings();
    SettingsStatus updateSettings(boolean sentimentAnalysisEnabled);
}
