package dev.jacid.hrApplication.application.services;

import org.springframework.stereotype.Service;

import dev.jacid.hrApplication.application.port.in.SettingsStatus;
import dev.jacid.hrApplication.application.port.in.SettingsUseCases;
import dev.jacid.hrApplication.application.port.out.SentimentAnalyzer;
import dev.jacid.hrApplication.application.port.out.SettingsRepository;
import dev.jacid.hrApplication.domain.model.AppSettings;
import jakarta.transaction.Transactional;

@Service
public class SettingsServiceImpl implements SettingsUseCases {

    private final SettingsRepository settingsRepository;
    private final SentimentAnalyzer sentimentAnalyzer;

    public SettingsServiceImpl(SettingsRepository settingsRepository, SentimentAnalyzer sentimentAnalyzer) {
        this.settingsRepository = settingsRepository;
        this.sentimentAnalyzer = sentimentAnalyzer;
    }

    @Override
    public SettingsStatus getSettings() {
        return status(settingsRepository.load());
    }

    @Override
    @Transactional
    public SettingsStatus updateSettings(boolean sentimentAnalysisEnabled) {
        return status(settingsRepository.save(new AppSettings(sentimentAnalysisEnabled)));
    }

    private SettingsStatus status(AppSettings settings) {
        return new SettingsStatus(settings.sentimentAnalysisEnabled(), sentimentAnalyzer.isAvailable());
    }
}
