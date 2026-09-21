package dev.jacid.hrApplication.application.port.out;

import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.SentimentLabel;

/**
 * Outbound port for business metrics about feedback. Implementations must be cheap and must not throw:
 * recording a metric never changes the outcome of a use case.
 */
public interface FeedbackMetrics {

    /** What happened to the sentiment analysis of a new feedback message. */
    enum SentimentOutcome {
        /** The message was analysed and got a label. */
        ANALYSED,
        /** Managers switched the analysis off, nothing was sent to the analyzer. */
        DISABLED,
        /** The analyzer is not configured (e.g. no API token). */
        UNAVAILABLE,
        /** The analyzer is configured but returned no result (error, timeout, unknown label). */
        FAILED
    }

    void feedbackCreated(Feedback feedback);

    /** {@code label} is only set for {@link SentimentOutcome#ANALYSED}. */
    void sentimentAnalysed(SentimentOutcome outcome, SentimentLabel label);

    /** Records nothing; for tests and for running without a metrics backend. */
    FeedbackMetrics NONE = new FeedbackMetrics() {
        @Override
        public void feedbackCreated(Feedback feedback) {
        }

        @Override
        public void sentimentAnalysed(SentimentOutcome outcome, SentimentLabel label) {
        }
    };
}
