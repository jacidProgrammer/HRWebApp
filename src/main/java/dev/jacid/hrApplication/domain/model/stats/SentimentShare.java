package dev.jacid.hrApplication.domain.model.stats;

/** Fraction (0..1) of feedback per sentiment; {@code notAnalysed} counts feedback without a sentiment. */
public record SentimentShare(double positive, double neutral, double negative, double notAnalysed) {
}
