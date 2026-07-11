package com.hqtraining.backend.model;

import java.math.BigDecimal;

public record LecturerStatisticsRecord(
        Long lecturerId,
        String lecturerNo,
        String lecturerName,
        Integer courseCount,
        Integer attendanceCount,
        BigDecimal averageRating,
        BigDecimal feeAmountTotal
) {
}
