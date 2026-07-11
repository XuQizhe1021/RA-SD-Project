package com.hqtraining.backend.model;

import java.math.BigDecimal;

public record StudentStatisticsRecord(
        Long studentId,
        String studentNo,
        String studentName,
        String companyName,
        Integer attendanceCount,
        Integer enrollmentCount,
        BigDecimal paidAmountTotal,
        BigDecimal averageRating
) {
}
