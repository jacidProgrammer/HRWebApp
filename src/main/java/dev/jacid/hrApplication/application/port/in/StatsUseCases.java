package dev.jacid.hrApplication.application.port.in;

import dev.jacid.hrApplication.domain.model.stats.StatsOverview;

public interface StatsUseCases {
    int DEFAULT_MONTHS = 6;
    int MAX_MONTHS = 12;

    /** Dashboard figures with a sentiment trend over the last {@code months} calendar months (1..12). */
    StatsOverview getOverview(int months);
}
