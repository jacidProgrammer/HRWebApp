package dev.jacid.hrApplication.adapter.in.http.dto;

import java.util.List;
import java.util.UUID;

/** JSON representation of {@code GET /stats/overview}. */
public record StatsOverviewDTO(int headcount,
                               List<DepartmentDTO> departments,
                               FeedbackVolumeDTO feedback,
                               SentimentShareDTO sentimentShare,
                               List<TrendDTO> trend,
                               List<ValueCountDTO> valueCounts,
                               List<RecognisedDTO> topRecognised,
                               List<AlertDTO> alerts) {

    public record DepartmentDTO(String name, int headcount) {}

    public record FeedbackVolumeDTO(int thisMonth, int lastMonth, long total) {}

    public record SentimentShareDTO(double positive, double neutral, double negative, double notAnalysed) {}

    /** {@code month} is formatted as {@code yyyy-MM}. */
    public record TrendDTO(String month, int positive, int neutral, int negative, int notAnalysed) {}

    public record ValueCountDTO(String value, int count) {}

    public record RecognisedDTO(UUID employeeId, String name, String department, int count, double positiveShare) {}

    public record AlertDTO(UUID employeeId, String name, String department, double previousPositiveShare,
                           double currentPositiveShare, int feedbackCount) {}
}
