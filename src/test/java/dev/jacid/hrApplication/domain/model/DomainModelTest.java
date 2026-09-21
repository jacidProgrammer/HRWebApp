package dev.jacid.hrApplication.domain.model;

import static dev.jacid.hrApplication.testsupport.TestData.JOSE;
import static dev.jacid.hrApplication.testsupport.TestData.LOUISA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;

import dev.jacid.hrApplication.domain.exception.InvalidEmployeeDataException;
import dev.jacid.hrApplication.domain.exception.InvalidFeedbackException;
import dev.jacid.hrApplication.domain.exception.InvalidRequestException;
import dev.jacid.hrApplication.testsupport.TestData;

class DomainModelTest {

    private static final Instant NOW = Instant.parse("2026-09-21T10:15:30Z");

    @Test
    void anonymousFeedbackHidesItsAuthor() {
        Feedback anonymous = TestData.feedback(LOUISA, JOSE, NOW, SentimentLabel.POSITIVE, FeedbackValue.CRAFT, true);

        Feedback hidden = anonymous.withAuthorHidden();

        assertThat(hidden.author()).isNull();
        assertThat(hidden.anonymous()).isTrue();
        assertThat(hidden).usingRecursiveComparison().ignoringFields("author").isEqualTo(anonymous);
    }

    @Test
    void namedFeedbackKeepsItsAuthor() {
        Feedback named = TestData.feedback(LOUISA, JOSE, NOW, SentimentLabel.POSITIVE);

        assertThat(named.withAuthorHidden()).isSameAs(named);
    }

    @Test
    void newEmployeesGetALowerCaseUsernameAndNoId() {
        Employee request = new Employee(null, "  Maria.Garcia ", "María", "Sales", "AE", "m@example.com", 1.0, "Mainz", null);

        Employee created = Employee.newFrom(request, NOW);

        assertThat(created.username()).isEqualTo("maria.garcia");
        assertThat(created.id()).isNull();
        assertThat(created.createdAt()).isEqualTo(NOW);
    }

    @Test
    void usernamesMustLookLikeKeycloakUsernames() {
        Employee spaces = new Employee(null, "maria garcia", "María", "Sales", "AE", "m@example.com", 1.0, "Mainz", null);

        assertThatThrownBy(spaces::requireComplete).isInstanceOf(InvalidEmployeeDataException.class);
    }

    @Test
    void employeesAreLinkedToUsersIgnoringCase() {
        assertThat(JOSE.isLinkedTo("JOSE")).isTrue();
        assertThat(new CurrentUser("Jose", Set.of(Role.EMPLOYEE)).owns(JOSE)).isTrue();
        assertThat(new CurrentUser("louisa", Set.of(Role.EMPLOYEE)).owns(JOSE)).isFalse();
        assertThat(CurrentUser.anonymous().owns(JOSE)).isFalse();
    }

    @Test
    void feedbackValuesAndSentimentFiltersAreParsedIgnoringCase() {
        assertThat(FeedbackValue.parse("customer_focus")).isEqualTo(FeedbackValue.CUSTOMER_FOCUS);
        assertThat(FeedbackValue.parse(null)).isNull();
        assertThat(SentimentFilter.parse("none")).isEqualTo(SentimentFilter.NONE);
        assertThat(SentimentFilter.NONE.label()).isNull();
        assertThat(SentimentFilter.NEGATIVE.label()).isEqualTo(SentimentLabel.NEGATIVE);

        assertThatThrownBy(() -> FeedbackValue.parse("KINDNESS")).isInstanceOf(InvalidFeedbackException.class);
        assertThatThrownBy(() -> SentimentFilter.parse("HAPPY")).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void feedbackFilterRejectsAnEmptyDateRange() {
        assertThatThrownBy(() -> new FeedbackFilter(null, null, NOW, NOW, null))
                .isInstanceOf(InvalidRequestException.class);
        assertThat(new FeedbackFilter(null, "  ", null, null, null).department()).isNull();
    }
}
