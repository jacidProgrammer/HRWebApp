package dev.jacid.hrApplication.domain.service;

import static dev.jacid.hrApplication.domain.model.SentimentLabel.NEGATIVE;
import static dev.jacid.hrApplication.domain.model.SentimentLabel.NEUTRAL;
import static dev.jacid.hrApplication.domain.model.SentimentLabel.POSITIVE;
import static dev.jacid.hrApplication.testsupport.TestData.JOSE;
import static dev.jacid.hrApplication.testsupport.TestData.LOUISA;
import static dev.jacid.hrApplication.testsupport.TestData.MARIA;
import static dev.jacid.hrApplication.testsupport.TestData.employee;
import static dev.jacid.hrApplication.testsupport.TestData.feedback;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.jacid.hrApplication.domain.model.Employee;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.FeedbackValue;
import dev.jacid.hrApplication.domain.model.SentimentLabel;
import dev.jacid.hrApplication.domain.model.stats.DepartmentHeadcount;
import dev.jacid.hrApplication.domain.model.stats.FeedbackVolume;
import dev.jacid.hrApplication.domain.model.stats.MonthlySentiment;
import dev.jacid.hrApplication.domain.model.stats.RecognisedEmployee;
import dev.jacid.hrApplication.domain.model.stats.SentimentAlert;
import dev.jacid.hrApplication.domain.model.stats.SentimentShare;
import dev.jacid.hrApplication.domain.model.stats.StatsOverview;
import dev.jacid.hrApplication.domain.model.stats.ValueCount;

class StatsCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");
    private static final List<Employee> EMPLOYEES = List.of(JOSE, LOUISA, MARIA);

    private static Instant daysAgo(double days) {
        return NOW.minus(Duration.ofMinutes(Math.round(days * 24 * 60)));
    }

    private static StatsOverview calculate(List<Feedback> feedback, int months) {
        return StatsCalculator.calculate(EMPLOYEES, feedback, feedback.size(), NOW, months);
    }

    /** {@code positive} positive and {@code others} negative feedback about {@code recipient}, {@code days} ago. */
    private static List<Feedback> received(Employee recipient, double days, int positive, int others) {
        List<Feedback> feedback = new ArrayList<>();
        for (int i = 0; i < positive + others; i++) {
            feedback.add(feedback(recipient, JOSE, daysAgo(days + i * 0.1), i < positive ? POSITIVE : NEGATIVE));
        }
        return feedback;
    }

    private static List<Feedback> concat(List<Feedback> a, List<Feedback> b) {
        List<Feedback> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    // --- trend -----------------------------------------------------------------------------------

    @Test
    void trendHasOneZeroFilledEntryPerMonthOldestFirst() {
        List<Feedback> feedback = List.of(
                feedback(LOUISA, JOSE, Instant.parse("2026-05-03T10:00:00Z"), POSITIVE),
                feedback(LOUISA, JOSE, Instant.parse("2026-05-20T10:00:00Z"), POSITIVE),
                feedback(MARIA, JOSE, Instant.parse("2026-09-02T10:00:00Z"), NEGATIVE),
                feedback(MARIA, JOSE, Instant.parse("2026-09-10T10:00:00Z"), null),
                feedback(MARIA, JOSE, Instant.parse("2026-03-31T23:59:59Z"), NEUTRAL)); // before the period

        List<MonthlySentiment> trend = calculate(feedback, 6).trend();

        assertThat(trend).containsExactly(
                new MonthlySentiment(YearMonth.of(2026, 4), 0, 0, 0, 0),
                new MonthlySentiment(YearMonth.of(2026, 5), 2, 0, 0, 0),
                new MonthlySentiment(YearMonth.of(2026, 6), 0, 0, 0, 0),
                new MonthlySentiment(YearMonth.of(2026, 7), 0, 0, 0, 0),
                new MonthlySentiment(YearMonth.of(2026, 8), 0, 0, 0, 0),
                new MonthlySentiment(YearMonth.of(2026, 9), 0, 0, 1, 1));
    }

    @Test
    void trendUsesUtcCalendarMonths() {
        List<Feedback> feedback = List.of(
                feedback(LOUISA, JOSE, Instant.parse("2026-08-31T23:59:59Z"), POSITIVE),
                feedback(LOUISA, JOSE, Instant.parse("2026-09-01T00:00:00Z"), NEUTRAL));

        assertThat(calculate(feedback, 2).trend()).containsExactly(
                new MonthlySentiment(YearMonth.of(2026, 8), 1, 0, 0, 0),
                new MonthlySentiment(YearMonth.of(2026, 9), 0, 1, 0, 0));
    }

    @Test
    void oneMonthPeriodIsTheCurrentMonthAndTwelveMonthsStartElevenMonthsAgo() {
        assertThat(calculate(List.of(), 1).trend()).extracting(MonthlySentiment::month)
                .containsExactly(YearMonth.of(2026, 9));
        assertThat(calculate(List.of(), 12).trend()).hasSize(12).first()
                .extracting(MonthlySentiment::month).isEqualTo(YearMonth.of(2025, 10));
    }

    @Test
    void monthsMustBePositive() {
        assertThatThrownBy(() -> calculate(List.of(), 0)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- headcount, volume, shares, values -------------------------------------------------------

    @Test
    void headcountAndDepartmentsSortedByName() {
        List<Employee> employees = List.of(JOSE, MARIA, LOUISA, employee("anna", "Anna", "Finance"));

        StatsOverview overview = StatsCalculator.calculate(employees, List.of(), 0, NOW, 6);

        assertThat(overview.headcount()).isEqualTo(4);
        assertThat(overview.departments()).containsExactly(
                new DepartmentHeadcount("Finance", 1), new DepartmentHeadcount("IT", 2), new DepartmentHeadcount("Sales", 1));
    }

    @Test
    void feedbackVolumeCountsCalendarMonthsAndUsesTheGivenTotal() {
        List<Feedback> feedback = List.of(
                feedback(LOUISA, JOSE, Instant.parse("2026-09-01T00:00:00Z"), POSITIVE),
                feedback(LOUISA, JOSE, Instant.parse("2026-09-21T11:00:00Z"), POSITIVE),
                feedback(LOUISA, JOSE, Instant.parse("2026-08-15T00:00:00Z"), POSITIVE),
                feedback(LOUISA, JOSE, Instant.parse("2026-07-31T23:00:00Z"), POSITIVE));

        StatsOverview overview = StatsCalculator.calculate(EMPLOYEES, feedback, 61, NOW, 6);

        assertThat(overview.feedback()).isEqualTo(new FeedbackVolume(2, 1, 61));
    }

    @Test
    void sentimentShareCoversThePeriodAndIsRounded() {
        List<Feedback> feedback = List.of(
                feedback(LOUISA, JOSE, daysAgo(1), POSITIVE),
                feedback(LOUISA, JOSE, daysAgo(2), POSITIVE),
                feedback(LOUISA, JOSE, daysAgo(3), NEUTRAL),
                feedback(LOUISA, JOSE, daysAgo(4), NEGATIVE),
                feedback(LOUISA, JOSE, daysAgo(5), POSITIVE),
                feedback(LOUISA, JOSE, daysAgo(6), null),
                feedback(LOUISA, JOSE, Instant.parse("2025-01-01T00:00:00Z"), NEGATIVE)); // outside 6 months

        assertThat(calculate(feedback, 6).sentimentShare()).isEqualTo(new SentimentShare(0.5, 0.17, 0.17, 0.17));
    }

    @Test
    void sharesAreZeroWithoutFeedback() {
        StatsOverview overview = calculate(List.of(), 6);

        assertThat(overview.sentimentShare()).isEqualTo(new SentimentShare(0, 0, 0, 0));
        assertThat(overview.topRecognised()).isEmpty();
        assertThat(overview.alerts()).isEmpty();
    }

    @Test
    void valueCountsListEveryValueMostFrequentFirst() {
        List<Feedback> feedback = List.of(
                feedback(LOUISA, JOSE, daysAgo(1), POSITIVE, FeedbackValue.GROWTH, false),
                feedback(LOUISA, JOSE, daysAgo(2), POSITIVE, FeedbackValue.GROWTH, false),
                feedback(LOUISA, JOSE, daysAgo(3), POSITIVE, FeedbackValue.CRAFT, false),
                feedback(LOUISA, JOSE, daysAgo(4), POSITIVE, null, false));

        assertThat(calculate(feedback, 6).valueCounts()).containsExactly(
                new ValueCount(FeedbackValue.GROWTH, 2),
                new ValueCount(FeedbackValue.CRAFT, 1),
                new ValueCount(FeedbackValue.TEAMWORK, 0),
                new ValueCount(FeedbackValue.OWNERSHIP, 0),
                new ValueCount(FeedbackValue.CUSTOMER_FOCUS, 0));
    }

    // --- top recognised --------------------------------------------------------------------------

    @Test
    void topRecognisedAreTheFiveMostMentionedEmployeesOfTheLast90Days() {
        List<Feedback> feedback = new ArrayList<>();
        String[] names = {"a", "b", "c", "d", "e", "f"};
        for (int i = 0; i < names.length; i++) {
            Employee employee = employee(names[i], names[i].toUpperCase(), "IT");
            for (int n = 0; n <= i; n++) {
                feedback.add(feedback(employee, JOSE, daysAgo(10 + n), POSITIVE));
            }
        }
        // 10 old items about "a" do not count
        for (int n = 0; n < 10; n++) {
            feedback.add(feedback(employee("a", "A", "IT"), JOSE, daysAgo(91 + n), POSITIVE));
        }

        assertThat(calculate(feedback, 6).topRecognised())
                .extracting(RecognisedEmployee::name, RecognisedEmployee::count)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("F", 6),
                        org.assertj.core.groups.Tuple.tuple("E", 5),
                        org.assertj.core.groups.Tuple.tuple("D", 4),
                        org.assertj.core.groups.Tuple.tuple("C", 3),
                        org.assertj.core.groups.Tuple.tuple("B", 2));
    }

    @Test
    void positiveShareOfRecognisedEmployeesIgnoresFeedbackThatWasNotAnalysed() {
        List<Feedback> feedback = List.of(
                feedback(LOUISA, JOSE, daysAgo(1), POSITIVE),
                feedback(LOUISA, JOSE, daysAgo(2), POSITIVE),
                feedback(LOUISA, JOSE, daysAgo(3), NEGATIVE),
                feedback(LOUISA, JOSE, daysAgo(4), null));

        assertThat(calculate(feedback, 6).topRecognised()).containsExactly(
                new RecognisedEmployee(LOUISA.id(), "Louisa", "IT", 4, 0.67));
    }

    // --- alerts ----------------------------------------------------------------------------------

    @Test
    void alertWhenThePositiveShareDropsByAtLeastAQuarter() {
        // previous 30 days: 4 of 5 positive (0.8); last 30 days: 2 of 5 positive (0.4)
        List<Feedback> feedback = concat(received(MARIA, 35, 4, 1), received(MARIA, 5, 2, 3));

        assertThat(calculate(feedback, 6).alerts()).containsExactly(
                new SentimentAlert(MARIA.id(), "Maria", "Sales", 0.8, 0.4, 5));
    }

    @Test
    void aDropOfExactlyAQuarterRaisesAnAlert() {
        // 4 of 4 (1.0) -> 3 of 4 (0.75)
        List<Feedback> feedback = concat(received(MARIA, 40, 4, 0), received(MARIA, 10, 3, 1));

        assertThat(calculate(feedback, 6).alerts()).singleElement()
                .satisfies(alert -> {
                    assertThat(alert.previousPositiveShare()).isEqualTo(1.0);
                    assertThat(alert.currentPositiveShare()).isEqualTo(0.75);
                });
    }

    @Test
    void aSmallerDropDoesNotRaiseAnAlert() {
        // 0.8 -> 0.6
        List<Feedback> feedback = concat(received(MARIA, 35, 4, 1), received(MARIA, 5, 3, 2));

        assertThat(calculate(feedback, 6).alerts()).isEmpty();
    }

    @Test
    void anImprovementDoesNotRaiseAnAlert() {
        List<Feedback> feedback = concat(received(MARIA, 35, 1, 4), received(MARIA, 5, 5, 0));

        assertThat(calculate(feedback, 6).alerts()).isEmpty();
    }

    @Test
    void alertsNeedThreeAnalysedFeedbackInEachWindow() {
        // big drop, but only 2 analysed items in the last 30 days (plus 2 not analysed)
        List<Feedback> current = new ArrayList<>(received(MARIA, 5, 0, 2));
        current.add(feedback(MARIA, JOSE, daysAgo(7), null));
        current.add(feedback(MARIA, JOSE, daysAgo(8), null));
        List<Feedback> feedback = concat(received(MARIA, 35, 5, 0), current);

        assertThat(calculate(feedback, 6).alerts()).isEmpty();

        // and the same the other way round: only 2 items in the previous window
        assertThat(calculate(concat(received(MARIA, 35, 2, 0), received(MARIA, 5, 0, 5)), 6).alerts()).isEmpty();
    }

    @Test
    void feedbackCountOfAnAlertIncludesFeedbackThatWasNotAnalysed() {
        List<Feedback> current = new ArrayList<>(received(MARIA, 5, 1, 2));
        current.add(feedback(MARIA, JOSE, daysAgo(9), null));
        List<Feedback> feedback = concat(received(MARIA, 35, 3, 0), current);

        assertThat(calculate(feedback, 6).alerts()).singleElement()
                .satisfies(alert -> {
                    assertThat(alert.currentPositiveShare()).isEqualTo(0.33);
                    assertThat(alert.feedbackCount()).isEqualTo(4);
                });
    }

    @Test
    void windowsAreTheLast30DaysAndThe30DaysBefore() {
        // 61 days ago is outside both windows: without it the previous window has only 2 items
        List<Feedback> feedback = concat(concat(received(MARIA, 31, 2, 0), received(MARIA, 61, 1, 0)), received(MARIA, 5, 0, 3));

        assertThat(calculate(feedback, 6).alerts()).isEmpty();
    }

    @Test
    void alertsAreSortedByTheSizeOfTheDrop() {
        List<Feedback> feedback = new ArrayList<>();
        feedback.addAll(concat(received(MARIA, 35, 4, 0), received(MARIA, 5, 2, 2)));   // 1.0 -> 0.5
        feedback.addAll(concat(received(LOUISA, 35, 4, 0), received(LOUISA, 5, 0, 4))); // 1.0 -> 0.0

        assertThat(calculate(feedback, 6).alerts()).extracting(SentimentAlert::name).containsExactly("Louisa", "Maria");
    }

    // --- loading window --------------------------------------------------------------------------

    @Test
    void earliestRelevantCoversThePeriodTheRecognitionWindowAndLastMonth() {
        assertThat(StatsCalculator.earliestRelevant(NOW, 6)).isEqualTo(Instant.parse("2026-04-01T00:00:00Z"));
        assertThat(StatsCalculator.earliestRelevant(NOW, 1)).isEqualTo(NOW.minus(Duration.ofDays(90)));
    }

    @Test
    void labelsAreCountedPerMonth() {
        List<Feedback> feedback = List.of(
                feedback(LOUISA, JOSE, daysAgo(1), SentimentLabel.POSITIVE),
                feedback(LOUISA, JOSE, daysAgo(2), SentimentLabel.NEUTRAL),
                feedback(LOUISA, JOSE, daysAgo(3), SentimentLabel.NEGATIVE));

        assertThat(calculate(feedback, 1).trend())
                .containsExactly(new MonthlySentiment(YearMonth.of(2026, 9), 1, 1, 1, 0));
    }
}
