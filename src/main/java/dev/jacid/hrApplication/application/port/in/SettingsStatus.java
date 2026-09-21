package dev.jacid.hrApplication.application.port.in;

/**
 * @param sentimentAnalysisEnabled   managers allow new feedback to be analysed
 * @param sentimentAnalysisAvailable the analysis service is configured (a Hugging Face token is set)
 */
public record SettingsStatus(boolean sentimentAnalysisEnabled, boolean sentimentAnalysisAvailable) {
}
