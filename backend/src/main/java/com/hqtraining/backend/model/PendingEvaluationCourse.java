package com.hqtraining.backend.model;

import java.time.LocalDateTime;

public record PendingEvaluationCourse(
        Long courseId,
        Long enrollmentId,
        String courseNo,
        String courseName,
        String lecturerName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String location
) {
}
