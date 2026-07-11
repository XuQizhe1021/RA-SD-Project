package com.hqtraining.backend.model;

import java.math.BigDecimal;

public record ExecutorStatisticsRecord(
        Long executorUserId,
        String executorName,
        Integer courseCount,
        Integer publishedCourseCount,
        Integer enrollmentReviewedCount,
        Integer trainingCompletedCount,
        Integer attendanceCount,
        BigDecimal paidAmountTotal
) {
}
