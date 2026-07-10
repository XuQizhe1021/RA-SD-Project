package com.hqtraining.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRecordView(
        Long id,
        Long enrollmentId,
        String enrollmentNo,
        Long courseId,
        String courseNo,
        String courseName,
        Long studentId,
        String studentNo,
        String studentName,
        String companyName,
        String paymentType,
        BigDecimal receivableAmount,
        BigDecimal paidAmount,
        String paymentMethod,
        String paymentStatus,
        LocalDateTime paidAt,
        Long handledBy,
        String handledByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
