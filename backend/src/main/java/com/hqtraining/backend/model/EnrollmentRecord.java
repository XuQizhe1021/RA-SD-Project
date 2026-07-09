package com.hqtraining.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EnrollmentRecord(
        Long id,
        String enrollmentNo,
        Long courseId,
        String courseNo,
        String courseName,
        String courseLocation,
        LocalDateTime courseStartTime,
        BigDecimal courseFeeAmount,
        Long studentId,
        String studentNo,
        String studentName,
        String companyName,
        String paymentType,
        String status,
        Long confirmedBy,
        String confirmedByName,
        LocalDateTime confirmedAt,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
