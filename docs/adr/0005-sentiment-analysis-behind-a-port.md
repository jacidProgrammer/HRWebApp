# 0005. Sentiment analysis behind a port, with graceful degradation

- Status: Accepted
- Date: 2026-09-21

## Context and problem statement

Feedback messages are classified as positive, neutral or negative with a Hugging Face model through its hosted
inference API. The API needs a token, can be slow (model cold start), rate-limits and fails. Sending feedback is the
core action of the product and must not depend on it.

## Considered options

- Call the Hugging Face API from the service and fail the request on errors
- Outbound port with an adapter that never throws; store feedback without sentiment when the analysis is not possible
- Asynchronous analysis (queue + worker) that fills the sentiment in later

## Decision outcome

Chosen option: **outbound port `SentimentAnalyzer` with graceful degradation**.

- The port returns `Optional<Sentiment>` and must not throw; `HuggingFaceSentimentAdapter` logs failures and returns
  empty. It also reports `isAvailable()` (token configured), which `GET /settings` exposes to the UI.
- `FeedbackServiceImpl` only calls it when managers enabled the analysis; otherwise, without a token or on failure,
  the feedback is stored with `sentiment: null`. The call happens outside a database transaction, with a 30 s timeout.
- Each outcome is counted (`hr_sentiment_analysis_total{outcome=analysed|disabled|unavailable|failed}`), so a
  silently failing integration is visible in Grafana.

### Consequences

- Good: sending feedback works with the external service down or unconfigured; the model or provider can be replaced
  with another adapter (for example a local model) without changing the use case.
- Bad: the request waits for the analysis (up to the timeout) and failed analyses are not retried; an asynchronous
  worker would fix both at the cost of a queue and eventual consistency, which this project does not need yet.
- Neutral: the dashboard counts "not analysed" feedback separately instead of guessing a label.
