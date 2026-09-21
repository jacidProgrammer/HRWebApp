package dev.jacid.hrApplication.domain.model.stats;

/** Number of feedback items in the current and the previous calendar month (UTC), and in total. */
public record FeedbackVolume(int thisMonth, int lastMonth, long total) {
}
