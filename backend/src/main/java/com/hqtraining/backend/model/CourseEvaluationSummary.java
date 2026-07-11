package com.hqtraining.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CourseEvaluationSummary(
        Long courseId,
        String courseNo,
        String courseName,
        String lecturerName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer shouldEvaluateCount,
        Integer evaluatedCount,
        BigDecimal averageRating
) {
}
