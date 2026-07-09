package com.hqtraining.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CourseOptionRecord(
        Long id,
        String courseNo,
        String courseName,
        String location,
        LocalDateTime startTime,
        Integer quota,
        BigDecimal feeAmount,
        String status
) {
}
