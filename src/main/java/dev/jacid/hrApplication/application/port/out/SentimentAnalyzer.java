package dev.jacid.hrApplication.application.port.out;

import java.util.Optional;

import dev.jacid.hrApplication.domain.model.Sentiment;

/**
 * Outbound port for sentiment analysis of free text.
 * Implementations must not throw: when the analysis is unavailable they return {@link Optional#empty()}.
 */
public interface SentimentAnalyzer {
    Optional<Sentiment> analyze(String text);
}
