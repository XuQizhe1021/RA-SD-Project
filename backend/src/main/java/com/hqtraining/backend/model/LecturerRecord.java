package com.hqtraining.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LecturerRecord(
        Long id,
        String lecturerNo,
        String fullName,
        String title,
        String specialty,
        String phone,
        String email,
        BigDecimal feeStandard,
        String profileText,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
