package dev.jacid.hrApplication.domain.model.stats;

import java.util.UUID;

/**
 * An employee who received a lot of feedback recently.
 *
 * @param positiveShare positive feedback divided by analysed feedback (0 when none was analysed)
 */
public record RecognisedEmployee(UUID employeeId, String name, String department, int count, double positiveShare) {
}
