package com.hqtraining.backend.model;

import java.time.LocalDateTime;

public record CourseNoticeRecord(
        Long id,
        Long courseId,
        String courseNo,
        String courseName,
        String title,
        String content,
        LocalDateTime registrationStartAt,
        LocalDateTime registrationEndAt,
        String status,
        LocalDateTime publishedAt,
        Boolean externalPublishFlag,
        Long createdBy,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
