package dev.jacid.hrApplication.domain.model.stats;

import dev.jacid.hrApplication.domain.model.FeedbackValue;

public record ValueCount(FeedbackValue value, int count) {
}
