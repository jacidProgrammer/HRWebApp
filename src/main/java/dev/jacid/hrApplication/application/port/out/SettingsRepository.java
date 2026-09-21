package dev.jacid.hrApplication.application.port.out;

import dev.jacid.hrApplication.domain.model.AppSettings;

public interface SettingsRepository {
    /** The stored settings, or {@link AppSettings#defaults()} when none are stored yet. */
    AppSettings load();
    AppSettings save(AppSettings settings);
}
