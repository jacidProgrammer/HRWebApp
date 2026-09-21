package dev.jacid.hrApplication.testsupport;

import java.time.Instant;
import java.util.UUID;

import dev.jacid.hrApplication.domain.model.Employee;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.FeedbackValue;
import dev.jacid.hrApplication.domain.model.Sentiment;
import dev.jacid.hrApplication.domain.model.SentimentLabel;

/** Domain objects shared by the unit tests. */
public final class TestData {

    public static final Instant CREATED = Instant.parse("2025-01-15T09:00:00Z");

    public static final Employee JOSE = employee("jose", "José Antonio", "IT");
    public static final Employee LOUISA = employee("louisa", "Louisa", "IT");
    public static final Employee MARIA = employee("maria", "Maria", "Sales");

    private TestData() {
    }

    public static Employee employee(String username, String name, String department) {
        return new Employee(UUID.nameUUIDFromBytes(username.getBytes()), username, name, department, "Role of " + name,
                username + "@example.com", 50000.0, "Mainz", CREATED);
    }

    public static Feedback feedback(Employee recipient, Employee author, Instant createdAt, SentimentLabel label) {
        return feedback(recipient, author, createdAt, label, null, false);
    }

    public static Feedback feedback(Employee recipient, Employee author, Instant createdAt, SentimentLabel label,
                                    FeedbackValue value, boolean anonymous) {
        return new Feedback(UUID.randomUUID(), recipient, author, anonymous, value, "Some feedback",
                label == null ? null : new Sentiment(label, 0.9), createdAt);
    }
}
