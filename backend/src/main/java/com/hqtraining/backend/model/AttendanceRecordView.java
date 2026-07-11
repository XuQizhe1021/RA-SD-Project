package com.hqtraining.backend.model;

import java.time.LocalDateTime;

public record AttendanceRecordView(
        Long id,
        Long enrollmentId,
        String enrollmentNo,
        Long courseId,
        String courseNo,
        String courseName,
        LocalDateTime courseStartTime,
        Long studentId,
        String studentNo,
        String studentName,
        String companyName,
        String attendanceStatus,
        LocalDateTime checkedInAt,
        Long checkedInBy,
        String checkedInByName,
        String remark,
        String materialStatus,
        String materialRemark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
