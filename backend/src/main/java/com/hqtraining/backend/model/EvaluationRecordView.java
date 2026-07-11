package com.hqtraining.backend.model;

import java.time.LocalDateTime;

public record EvaluationRecordView(
        Long id,
        Long courseId,
        Long enrollmentId,
        String courseNo,
        String courseName,
        String lecturerName,
        Long studentId,
        String studentName,
        String companyName,
        Integer rating,
        String commentText,
        String source,
        Long proxyStaffId,
        String proxyStaffName,
        Long submittedBy,
        String submittedByName,
        LocalDateTime submittedAt
) {
}
