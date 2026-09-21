package dev.jacid.hrApplication.adapter.out.metrics;

import static dev.jacid.hrApplication.testsupport.TestData.JOSE;
import static dev.jacid.hrApplication.testsupport.TestData.LOUISA;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import dev.jacid.hrApplication.application.port.out.FeedbackMetrics.SentimentOutcome;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.FeedbackValue;
import dev.jacid.hrApplication.domain.model.SentimentLabel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class MicrometerFeedbackMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MicrometerFeedbackMetrics metrics = new MicrometerFeedbackMetrics(registry);

    @Test
    void countsCreatedFeedbackByAnonymityAndValue() {
        metrics.feedbackCreated(feedback(true, FeedbackValue.CUSTOMER_FOCUS));
        metrics.feedbackCreated(feedback(true, FeedbackValue.CUSTOMER_FOCUS));
        metrics.feedbackCreated(feedback(false, null));

        assertThat(count(MicrometerFeedbackMetrics.FEEDBACK_SUBMITTED, "anonymous", "true", "value", "customer_focus")).isEqualTo(2);
        assertThat(count(MicrometerFeedbackMetrics.FEEDBACK_SUBMITTED, "anonymous", "false", "value", "none")).isEqualTo(1);
    }

    @Test
    void countsSentimentOutcomesWithTheirLabel() {
        metrics.sentimentAnalysed(SentimentOutcome.ANALYSED, SentimentLabel.NEGATIVE);
        metrics.sentimentAnalysed(SentimentOutcome.FAILED, null);
        metrics.sentimentAnalysed(SentimentOutcome.FAILED, null);

        assertThat(count(MicrometerFeedbackMetrics.SENTIMENT_ANALYSIS, "outcome", "analysed", "label", "negative")).isEqualTo(1);
        assertThat(count(MicrometerFeedbackMetrics.SENTIMENT_ANALYSIS, "outcome", "failed", "label", "none")).isEqualTo(2);
    }

    private double count(String name, String... tags) {
        return registry.get(name).tags(tags).counter().count();
    }

    private static Feedback feedback(boolean anonymous, FeedbackValue value) {
        return new Feedback(null, JOSE, LOUISA, anonymous, value, "Thanks!", null, Instant.parse("2026-09-21T10:00:00Z"));
    }
}
