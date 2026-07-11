package com.hqtraining.backend.model;

import java.time.LocalDateTime;

public record RegistrationReviewRecord(
        Long userId,
        String username,
        String displayName,
        String phone,
        String email,
        String companyName,
        String jobTitle,
        String educationLevel,
        String techLevel,
        String accountStatus,
        String reviewerName,
        String reviewComment,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
}
