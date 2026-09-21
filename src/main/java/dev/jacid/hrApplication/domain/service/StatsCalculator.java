package dev.jacid.hrApplication.domain.service;

import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

/**
 * Computes the manager dashboard figures from plain domain objects (no I/O, fully deterministic for a
 * given {@code now}). Calendar months are UTC months.
 * <ul>
 *   <li>{@code trend}: one entry per month of the selected period (the current month and the
 *       {@code months - 1} before it), oldest first, zero-filled.</li>
 *   <li>{@code sentimentShare} and {@code valueCounts}: feedback created in the same period.</li>
 *   <li>{@code topRecognised}: the {@value #TOP_RECOGNISED_LIMIT} employees with most feedback received in the
 *       last 90 days.</li>
 *   <li>{@code alerts}: employees whose positive share (positive / analysed) in the last 30 days dropped by at
 *       least {@value #ALERT_MIN_DROP} compared with the 30 days before, with at least
 *       {@value #ALERT_MIN_ANALYSED} analysed feedback items in each window.</li>
 * </ul>
 * Shares are rounded to two decimals.
 */
public final class StatsCalculator {

    public static final int TOP_RECOGNISED_LIMIT = 5;
    public static final int ALERT_MIN_ANALYSED = 3;
    public static final double ALERT_MIN_DROP = 0.25;
    static final Duration RECOGNITION_WINDOW = Duration.ofDays(90);
    static final Duration ALERT_WINDOW = Duration.ofDays(30);
    /** Tolerance for comparing the drop with {@link #ALERT_MIN_DROP} (0.8 - 0.55 is not exactly 0.25 in binary). */
    private static final double EPSILON = 1e-9;

    private StatsCalculator() {
    }

    /** Oldest creation date the calculation looks at; older feedback only contributes to the total. */
    public static Instant earliestRelevant(Instant now, int months) {
        Instant periodStart = firstMonth(now, months).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant lastMonthStart = YearMonth.from(now.atOffset(ZoneOffset.UTC)).minusMonths(1)
                .atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return Stream.of(periodStart, lastMonthStart, now.minus(RECOGNITION_WINDOW), now.minus(ALERT_WINDOW.multipliedBy(2)))
                .min(Comparator.naturalOrder())
                .orElseThrow();
    }

    /**
     * @param employees     all employees
     * @param feedback      at least every feedback item created since {@link #earliestRelevant(Instant, int)}
     * @param totalFeedback number of feedback items ever stored
     * @param months        length of the trend period, at least 1
     */
    public static StatsOverview calculate(List<Employee> employees, List<Feedback> feedback, long totalFeedback,
                                          Instant now, int months) {
        if (months < 1) {
            throw new IllegalArgumentException("months must be at least 1");
        }
        YearMonth currentMonth = monthOf(now);
        YearMonth firstMonth = firstMonth(now, months);
        List<Feedback> inPeriod = feedback.stream().filter(f -> !monthOf(f.createdAt()).isBefore(firstMonth)).toList();

        return new StatsOverview(
                employees.size(),
                departments(employees),
                new FeedbackVolume(countInMonth(feedback, currentMonth), countInMonth(feedback, currentMonth.minusMonths(1)),
                        totalFeedback),
                sentimentShare(inPeriod),
                trend(inPeriod, firstMonth, months),
                valueCounts(inPeriod),
                topRecognised(feedback, now),
                alerts(feedback, now));
    }

    private static List<DepartmentHeadcount> departments(List<Employee> employees) {
        Map<String, Long> byDepartment = employees.stream()
                .collect(Collectors.groupingBy(Employee::department, TreeMap::new, Collectors.counting()));
        return byDepartment.entrySet().stream()
                .map(entry -> new DepartmentHeadcount(entry.getKey(), entry.getValue().intValue()))
                .toList();
    }

    private static int countInMonth(List<Feedback> feedback, YearMonth month) {
        return (int) feedback.stream().filter(f -> monthOf(f.createdAt()).equals(month)).count();
    }

    private static SentimentShare sentimentShare(List<Feedback> feedback) {
        int total = feedback.size();
        if (total == 0) {
            return new SentimentShare(0, 0, 0, 0);
        }
        return new SentimentShare(
                round(count(feedback, f -> f.hasLabel(SentimentLabel.POSITIVE)) / (double) total),
                round(count(feedback, f -> f.hasLabel(SentimentLabel.NEUTRAL)) / (double) total),
                round(count(feedback, f -> f.hasLabel(SentimentLabel.NEGATIVE)) / (double) total),
                round(count(feedback, f -> !f.isAnalysed()) / (double) total));
    }

    private static List<MonthlySentiment> trend(List<Feedback> feedback, YearMonth firstMonth, int months) {
        Map<YearMonth, List<Feedback>> byMonth = new LinkedHashMap<>();
        for (int i = 0; i < months; i++) {
            byMonth.put(firstMonth.plusMonths(i), new ArrayList<>());
        }
        feedback.forEach(f -> {
            List<Feedback> bucket = byMonth.get(monthOf(f.createdAt()));
            if (bucket != null) {
                bucket.add(f);
            }
        });
        return byMonth.entrySet().stream()
                .map(entry -> new MonthlySentiment(entry.getKey(),
                        count(entry.getValue(), f -> f.hasLabel(SentimentLabel.POSITIVE)),
                        count(entry.getValue(), f -> f.hasLabel(SentimentLabel.NEUTRAL)),
                        count(entry.getValue(), f -> f.hasLabel(SentimentLabel.NEGATIVE)),
                        count(entry.getValue(), f -> !f.isAnalysed())))
                .toList();
    }

    /** Every value, most frequent first (zero counts included, ties in declaration order). */
    private static List<ValueCount> valueCounts(List<Feedback> feedback) {
        Map<FeedbackValue, Integer> counts = new EnumMap<>(FeedbackValue.class);
        Arrays.stream(FeedbackValue.values()).forEach(value -> counts.put(value, 0));
        feedback.stream().filter(f -> f.value() != null).forEach(f -> counts.merge(f.value(), 1, Integer::sum));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<FeedbackValue, Integer>comparingByValue().reversed())
                .map(entry -> new ValueCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static List<RecognisedEmployee> topRecognised(List<Feedback> feedback, Instant now) {
        Instant since = now.minus(RECOGNITION_WINDOW);
        return byRecipient(feedback, f -> f.createdAt().isAfter(since) && !f.createdAt().isAfter(now))
                .values().stream()
                .map(received -> {
                    Employee recipient = received.get(0).recipient();
                    return new RecognisedEmployee(recipient.id(), recipient.name(), recipient.department(),
                            received.size(), round(positiveShare(received)));
                })
                .sorted(Comparator.comparingInt(RecognisedEmployee::count).reversed()
                        .thenComparing(Comparator.comparingDouble(RecognisedEmployee::positiveShare).reversed())
                        .thenComparing(RecognisedEmployee::name))
                .limit(TOP_RECOGNISED_LIMIT)
                .toList();
    }

    private static List<SentimentAlert> alerts(List<Feedback> feedback, Instant now) {
        Instant currentStart = now.minus(ALERT_WINDOW);
        Instant previousStart = currentStart.minus(ALERT_WINDOW);
        Map<UUID, List<Feedback>> current = byRecipient(feedback, f -> within(f, currentStart, now));
        Map<UUID, List<Feedback>> previous = byRecipient(feedback, f -> within(f, previousStart, currentStart));

        List<SentimentAlert> alerts = new ArrayList<>();
        current.forEach((employeeId, now30) -> {
            List<Feedback> before30 = previous.getOrDefault(employeeId, List.of());
            if (count(now30, Feedback::isAnalysed) < ALERT_MIN_ANALYSED || count(before30, Feedback::isAnalysed) < ALERT_MIN_ANALYSED) {
                return;
            }
            double previousShare = positiveShare(before30);
            double currentShare = positiveShare(now30);
            if (previousShare - currentShare >= ALERT_MIN_DROP - EPSILON) {
                Employee recipient = now30.get(0).recipient();
                alerts.add(new SentimentAlert(employeeId, recipient.name(), recipient.department(),
                        round(previousShare), round(currentShare), now30.size()));
            }
        });
        alerts.sort(Comparator.comparingDouble((SentimentAlert a) -> a.previousPositiveShare() - a.currentPositiveShare())
                .reversed()
                .thenComparing(SentimentAlert::name));
        return alerts;
    }

    /** Created in {@code (start, end]}. */
    private static boolean within(Feedback feedback, Instant start, Instant end) {
        return feedback.createdAt().isAfter(start) && !feedback.createdAt().isAfter(end);
    }

    private static Map<UUID, List<Feedback>> byRecipient(List<Feedback> feedback, Predicate<Feedback> filter) {
        return feedback.stream()
                .filter(f -> f.recipient() != null)
                .filter(filter)
                .collect(Collectors.groupingBy(f -> f.recipient().id(), LinkedHashMap::new, Collectors.toList()));
    }

    /** Positive feedback divided by analysed feedback; 0 when nothing was analysed. */
    private static double positiveShare(List<Feedback> feedback) {
        int analysed = count(feedback, Feedback::isAnalysed);
        return analysed == 0 ? 0 : count(feedback, f -> f.hasLabel(SentimentLabel.POSITIVE)) / (double) analysed;
    }

    private static int count(List<Feedback> feedback, Predicate<Feedback> predicate) {
        return (int) feedback.stream().filter(predicate).count();
    }

    private static YearMonth firstMonth(Instant now, int months) {
        return monthOf(now).minusMonths(months - 1L);
    }

    private static YearMonth monthOf(Instant instant) {
        return YearMonth.from(instant.atOffset(ZoneOffset.UTC));
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
