package com.hqtraining.backend.model;

import java.time.LocalDateTime;

public record EvaluationCandidateRecord(
        Long courseId,
        Long enrollmentId,
        Long studentId,
        String studentNo,
        String studentName,
        String companyName,
        LocalDateTime checkedInAt,
        String evaluationStatus,
        String evaluationSource,
        LocalDateTime submittedAt
) {
}
