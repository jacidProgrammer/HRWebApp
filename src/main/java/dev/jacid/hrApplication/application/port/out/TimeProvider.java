package dev.jacid.hrApplication.application.port.out;

import java.time.Instant;

/** Current time, replaceable in tests. */
@FunctionalInterface
public interface TimeProvider {
    Instant now();
}
