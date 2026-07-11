package com.hqtraining.backend.model;

import java.time.LocalDateTime;

public record StudentProfileRecord(
        Long id,
        Long userId,
        String studentNo,
        String fullName,
        String gender,
        Long companyId,
        String companyName,
        String jobTitle,
        String educationLevel,
        String techLevel,
        String phone,
        String email,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
