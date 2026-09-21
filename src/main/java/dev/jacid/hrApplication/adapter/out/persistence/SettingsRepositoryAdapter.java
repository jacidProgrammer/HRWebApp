package dev.jacid.hrApplication.adapter.out.persistence;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import dev.jacid.hrApplication.application.port.out.SettingsRepository;
import dev.jacid.hrApplication.domain.model.AppSettings;

@Repository
@Profile({"h2","postgres","test"})
public class SettingsRepositoryAdapter implements SettingsRepository {

    private final AppSettingsRepositoryJpa repository;

    public SettingsRepositoryAdapter(AppSettingsRepositoryJpa repository) {
        this.repository = repository;
    }

    @Override
    public AppSettings load() {
        return repository.findById(AppSettingsJpaEntity.SINGLETON_ID)
                .map(entity -> new AppSettings(entity.isSentimentAnalysisEnabled()))
                .orElseGet(AppSettings::defaults);
    }

    @Override
    public AppSettings save(AppSettings settings) {
        AppSettingsJpaEntity entity = new AppSettingsJpaEntity();
        entity.setId(AppSettingsJpaEntity.SINGLETON_ID);
        entity.setSentimentAnalysisEnabled(settings.sentimentAnalysisEnabled());
        AppSettingsJpaEntity saved = repository.save(entity);
        return new AppSettings(saved.isSentimentAnalysisEnabled());
    }
}
