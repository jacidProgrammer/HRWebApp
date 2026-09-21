package dev.jacid.hrApplication.domain.model.stats;

import java.util.List;

/** Aggregated figures for the manager dashboard. Contains no individual feedback content. */
public record StatsOverview(int headcount,
                            List<DepartmentHeadcount> departments,
                            FeedbackVolume feedback,
                            SentimentShare sentimentShare,
                            List<MonthlySentiment> trend,
                            List<ValueCount> valueCounts,
                            List<RecognisedEmployee> topRecognised,
                            List<SentimentAlert> alerts) {
}
