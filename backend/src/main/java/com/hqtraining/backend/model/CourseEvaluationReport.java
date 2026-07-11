package com.hqtraining.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CourseEvaluationReport(
        Long courseId,
        String courseNo,
        String courseName,
        String lecturerName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        Integer shouldEvaluateCount,
        Integer evaluatedCount,
        BigDecimal participationRate,
        BigDecimal averageRating,
        List<ScoreDistributionItem> scoreDistribution,
        List<EvaluationRecordView> details
) {
}
