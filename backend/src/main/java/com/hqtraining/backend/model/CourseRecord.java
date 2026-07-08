package com.hqtraining.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CourseRecord(
        Long id,
        String courseNo,
        Long applicationId,
        String courseName,
        Long lecturerId,
        String lecturerName,
        Long executorUserId,
        String executorName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location,
        Integer quota,
        BigDecimal feeAmount,
        String status,
        String sourceType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
