package dev.jacid.hrApplication.adapter.in.http.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/** JSON representation of {@code GET /stats/overview}. */
@Schema(description = "Manager dashboard figures")
public record StatsOverviewDTO(@Schema(description = "Number of employees") int headcount,
                               @Schema(description = "Headcount per department") List<DepartmentDTO> departments,
                               FeedbackVolumeDTO feedback,
                               @Schema(description = "Sentiment share in the selected months") SentimentShareDTO sentimentShare,
                               @Schema(description = "One entry per month of the selected period, oldest first, zero-filled") List<TrendDTO> trend,
                               @Schema(description = "Every company value with its count in the selected period, most frequent first") List<ValueCountDTO> valueCounts,
                               @Schema(description = "The 5 employees with most feedback received in the last 90 days") List<RecognisedDTO> topRecognised,
                               @Schema(description = """
                                       Employees whose positive share in the last 30 days dropped by at least 0.25 compared \
                                       with the 30 days before, with at least 3 analysed items in each window""") List<AlertDTO> alerts) {

    public record DepartmentDTO(String name, int headcount) {}

    @Schema(description = "Feedback created this month, last month and in total")
    public record FeedbackVolumeDTO(int thisMonth, int lastMonth, long total) {}

    @Schema(description = "Shares between 0 and 1, rounded to two decimals")
    public record SentimentShareDTO(double positive, double neutral, double negative, double notAnalysed) {}

    public record TrendDTO(@Schema(description = "Calendar month (UTC)", example = "2026-09") String month,
                           int positive, int neutral, int negative, int notAnalysed) {}

    public record ValueCountDTO(@Schema(allowableValues = {"TEAMWORK", "OWNERSHIP", "CRAFT", "CUSTOMER_FOCUS", "GROWTH"}) String value,
                                int count) {}

    public record RecognisedDTO(UUID employeeId, String name, String department, int count,
                                @Schema(description = "Positive / analysed feedback, 0..1") double positiveShare) {}

    @Schema(description = "Aggregated alert: no message content")
    public record AlertDTO(UUID employeeId, String name, String department, double previousPositiveShare,
                           double currentPositiveShare, @Schema(description = "Feedback items in the last 30 days") int feedbackCount) {}
}
