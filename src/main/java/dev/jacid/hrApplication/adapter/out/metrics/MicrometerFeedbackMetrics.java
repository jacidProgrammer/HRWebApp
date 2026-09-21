package dev.jacid.hrApplication.adapter.out.metrics;

import java.util.Locale;

import org.springframework.stereotype.Component;

import dev.jacid.hrApplication.application.port.out.FeedbackMetrics;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.SentimentLabel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * {@link FeedbackMetrics} as Micrometer counters, exported at {@code /actuator/prometheus} as
 * {@code hr_feedback_submitted_total} and {@code hr_sentiment_analysis_total}.
 * Tags only carry low-cardinality values: never ids, names or message content.
 */
@Component
public class MicrometerFeedbackMetrics implements FeedbackMetrics {

    static final String FEEDBACK_SUBMITTED = "hr.feedback.submitted";
    static final String SENTIMENT_ANALYSIS = "hr.sentiment.analysis";

    private final MeterRegistry registry;

    public MicrometerFeedbackMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void feedbackCreated(Feedback feedback) {
        Counter.builder(FEEDBACK_SUBMITTED)
                .description("Feedback items created")
                .tag("anonymous", String.valueOf(feedback.anonymous()))
                .tag("value", feedback.value() == null ? "none" : lower(feedback.value().name()))
                .register(registry)
                .increment();
    }

    @Override
    public void sentimentAnalysed(SentimentOutcome outcome, SentimentLabel label) {
        Counter.builder(SENTIMENT_ANALYSIS)
                .description("Sentiment analysis attempts for new feedback, by outcome and label")
                .tag("outcome", lower(outcome.name()))
                .tag("label", label == null ? "none" : lower(label.name()))
                .register(registry)
                .increment();
    }

    private static String lower(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
