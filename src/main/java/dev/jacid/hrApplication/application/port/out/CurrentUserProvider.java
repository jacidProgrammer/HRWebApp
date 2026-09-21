package dev.jacid.hrApplication.application.port.out;

import dev.jacid.hrApplication.domain.model.CurrentUser;

/** Outbound port that tells the application who is performing the current request. */
public interface CurrentUserProvider {
    CurrentUser currentUser();
}
