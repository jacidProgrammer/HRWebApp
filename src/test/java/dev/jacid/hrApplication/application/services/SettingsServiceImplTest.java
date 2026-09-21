package dev.jacid.hrApplication.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import dev.jacid.hrApplication.application.port.in.SettingsStatus;
import dev.jacid.hrApplication.application.port.out.SentimentAnalyzer;
import dev.jacid.hrApplication.application.port.out.SettingsRepository;
import dev.jacid.hrApplication.domain.model.AppSettings;

class SettingsServiceImplTest {

    private final SettingsRepository repository = mock(SettingsRepository.class);
    private final SentimentAnalyzer analyzer = mock(SentimentAnalyzer.class);
    private final SettingsServiceImpl service = new SettingsServiceImpl(repository, analyzer);

    @Test
    void settingsCombineTheStoredFlagWithTheAnalyzerAvailability() {
        given(repository.load()).willReturn(new AppSettings(true));
        given(analyzer.isAvailable()).willReturn(false);

        assertThat(service.getSettings()).isEqualTo(new SettingsStatus(true, false));
    }

    @Test
    void updateStoresTheFlag() {
        given(repository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(analyzer.isAvailable()).willReturn(true);

        assertThat(service.updateSettings(false)).isEqualTo(new SettingsStatus(false, true));
        then(repository).should().save(new AppSettings(false));
    }
}
