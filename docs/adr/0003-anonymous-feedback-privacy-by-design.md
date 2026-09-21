# 0003. Anonymous feedback and privacy by design

- Status: Accepted
- Date: 2026-09-21

## Context and problem statement

Peer feedback and sentiment scores derived from it are personal data about employees and can be used to assess
them. In Germany, a tool able to monitor behaviour or performance is subject to works council co-determination
(§87(1) no. 6 BetrVG), and automated analysis of employee feedback calls for a data protection impact assessment
(GDPR Art. 35). The product still has to give managers useful signals.

## Considered options

- Show managers everything (authors, per-person sentiment history)
- Minimise by design: anonymity that holds for managers too, aggregated insights, an off switch for the AI
- No sentiment analysis at all

## Decision outcome

Chosen option: **minimise by design**:

- **Anonymous means anonymous for everybody but the author.** The author is stored only to enforce rules
  (no feedback to yourself) and is removed from every response except the author's own (`Feedback.withAuthorHidden`).
- **Aggregated alerts.** The dashboard shows counts and shares. An alert only says that the share of positive
  feedback about someone dropped by at least 0.25 between two 30-day windows, requires at least 3 analysed items in
  each window (one message cannot trigger it) and carries no message content.
- **AI toggle.** Managers can switch the sentiment analysis off at runtime (`PUT /settings`); while it is off,
  or without a token, nothing leaves the system. Only the message text is sent, never ids or names.
- **Least visibility.** Employees only read feedback they received or sent, and salary/address only of themselves.
  Deleting an employee deletes the feedback about them and anonymises what they wrote.
- **Metrics without personal data.** The Prometheus counters are tagged with low-cardinality values only.

### Consequences

- Good: the defaults are defensible in a works council or DPIA discussion; the rules live in the domain and are unit tested.
- Bad: managers cannot follow up with the author of anonymous feedback, by design.
- Neutral: this is a technical design, not legal advice; a real rollout still needs the organisational steps.
