package com.hqtraining.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CourseStatisticsRecord(
        Long courseId,
        String courseNo,
        String courseName,
        String lecturerName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer quota,
        Integer enrollmentCount,
        Integer attendanceCount,
        BigDecimal paidAmountTotal,
        BigDecimal averageRating
) {
}
