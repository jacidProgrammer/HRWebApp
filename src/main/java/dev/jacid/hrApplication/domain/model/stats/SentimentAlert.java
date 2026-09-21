package dev.jacid.hrApplication.domain.model.stats;

import java.util.UUID;

/**
 * The share of positive feedback about an employee dropped noticeably between two 30-day windows.
 *
 * @param feedbackCount number of feedback items about the employee in the current (last 30 days) window
 */
public record SentimentAlert(UUID employeeId, String name, String department,
                             double previousPositiveShare, double currentPositiveShare, int feedbackCount) {
}
