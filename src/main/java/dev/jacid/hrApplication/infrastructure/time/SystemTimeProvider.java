package dev.jacid.hrApplication.infrastructure.time;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import dev.jacid.hrApplication.application.port.out.TimeProvider;

/** System clock, truncated to whole seconds so stored and returned timestamps look like {@code 2026-09-21T10:15:30Z}. */
@Component
public class SystemTimeProvider implements TimeProvider {

    @Override
    public Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
