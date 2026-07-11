package com.hqtraining.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RevenueDetailRecord(
        Long paymentId,
        String courseName,
        String studentName,
        BigDecimal receivableAmount,
        BigDecimal paidAmount,
        String paymentMethod,
        LocalDateTime paidAt,
        String handledByName
) {
}
