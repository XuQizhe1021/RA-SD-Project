package com.hqtraining.backend.model;

import java.time.LocalDateTime;

public record ManagedAccountRecord(
        Long userId,
        String username,
        String displayName,
        String phone,
        String email,
        String accountType,
        String roleCode,
        String roleName,
        String accountStatus,
        String reviewerName,
        String reviewComment,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        LocalDateTime lastLoginAt
) {
}
