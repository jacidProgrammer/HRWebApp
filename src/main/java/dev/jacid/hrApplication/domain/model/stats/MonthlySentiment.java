package dev.jacid.hrApplication.domain.model.stats;

import java.time.YearMonth;

/** Number of feedback items per sentiment created in one calendar month (UTC). */
public record MonthlySentiment(YearMonth month, int positive, int neutral, int negative, int notAnalysed) {
}
