package com.hqtraining.backend.service;

import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.AttendanceCheckInRequest;
import com.hqtraining.backend.model.AttendanceRecordView;
import com.hqtraining.backend.model.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AttendanceService {

    private static final RowMapper<AttendanceRecordView> ATTENDANCE_ROW_MAPPER = (rs, rowNum) -> new AttendanceRecordView(
            rs.getLong("id"),
            rs.getLong("enrollment_id"),
            rs.getString("enrollment_no"),
            rs.getLong("course_id"),
            rs.getString("course_no"),
            rs.getString("course_name"),
            rs.getTimestamp("start_time").toLocalDateTime(),
            rs.getLong("student_id"),
            rs.getString("student_no"),
            rs.getString("student_name"),
            rs.getString("company_name"),
            rs.getString("status"),
            toLocalDateTime(rs.getTimestamp("checked_in_at")),
            rs.getObject("checked_in_by", Long.class),
            rs.getString("checked_in_by_name"),
            rs.getString("remark"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public AttendanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<AttendanceRecordView> getAttendanceRecords(
            int pageNum,
            int pageSize,
            String keyword,
            String status,
            Long courseId,
            CurrentUser currentUser
    ) {
        ensureSiteStaff(currentUser);
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        String likeKeyword = normalizeLikeKeyword(keyword);

        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM attendance_record ar
                INNER JOIN enrollment e ON ar.enrollment_id = e.id
                INNER JOIN course c ON ar.course_id = c.id
                INNER JOIN student_profile s ON ar.student_id = s.id
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                WHERE (? IS NULL OR ar.status = ?)
                  AND (? IS NULL OR ar.course_id = ?)
                  AND (
                    ? IS NULL
                    OR e.enrollment_no LIKE ?
                    OR c.course_no LIKE ?
                    OR c.course_name LIKE ?
                    OR s.student_no LIKE ?
                    OR s.full_name LIKE ?
                    OR COALESCE(cc.company_name, '') LIKE ?
                  )
                """,
                Long.class,
                emptyToNull(status), emptyToNull(status),
                courseId, courseId,
                likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword
        );

        List<AttendanceRecordView> list = jdbcTemplate.query(
                """
                SELECT
                    ar.id,
                    ar.enrollment_id,
                    e.enrollment_no,
                    ar.course_id,
                    c.course_no,
                    c.course_name,
                    c.start_time,
                    ar.student_id,
                    s.student_no,
                    s.full_name AS student_name,
                    COALESCE(cc.company_name, '') AS company_name,
                    ar.status,
                    ar.checked_in_at,
                    ar.checked_in_by,
                    COALESCE(u.display_name, '') AS checked_in_by_name,
                    ar.remark,
                    ar.created_at,
                    ar.updated_at
                FROM attendance_record ar
                INNER JOIN enrollment e ON ar.enrollment_id = e.id
                INNER JOIN course c ON ar.course_id = c.id
                INNER JOIN student_profile s ON ar.student_id = s.id
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                LEFT JOIN user_account u ON ar.checked_in_by = u.id
                WHERE (? IS NULL OR ar.status = ?)
                  AND (? IS NULL OR ar.course_id = ?)
                  AND (
                    ? IS NULL
                    OR e.enrollment_no LIKE ?
                    OR c.course_no LIKE ?
                    OR c.course_name LIKE ?
                    OR s.student_no LIKE ?
                    OR s.full_name LIKE ?
                    OR COALESCE(cc.company_name, '') LIKE ?
                  )
                ORDER BY c.start_time ASC, ar.id ASC
                LIMIT ? OFFSET ?
                """,
                ATTENDANCE_ROW_MAPPER,
                emptyToNull(status), emptyToNull(status),
                courseId, courseId,
                likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword,
                safePageSize, (safePageNum - 1) * safePageSize
        );

        return new PageResult<>(list, safePageNum, safePageSize, total == null ? 0 : total);
    }

    public AttendanceRecordView getAttendanceRecordById(Long id, CurrentUser currentUser) {
        ensureSiteStaff(currentUser);
        List<AttendanceRecordView> list = jdbcTemplate.query(
                """
                SELECT
                    ar.id,
                    ar.enrollment_id,
                    e.enrollment_no,
                    ar.course_id,
                    c.course_no,
                    c.course_name,
                    c.start_time,
                    ar.student_id,
                    s.student_no,
                    s.full_name AS student_name,
                    COALESCE(cc.company_name, '') AS company_name,
                    ar.status,
                    ar.checked_in_at,
                    ar.checked_in_by,
                    COALESCE(u.display_name, '') AS checked_in_by_name,
                    ar.remark,
                    ar.created_at,
                    ar.updated_at
                FROM attendance_record ar
                INNER JOIN enrollment e ON ar.enrollment_id = e.id
                INNER JOIN course c ON ar.course_id = c.id
                INNER JOIN student_profile s ON ar.student_id = s.id
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                LEFT JOIN user_account u ON ar.checked_in_by = u.id
                WHERE ar.id = ?
                """,
                ATTENDANCE_ROW_MAPPER,
                id
        );
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "签到记录不存在");
        }
        return list.get(0);
    }

    public AttendanceRecordView checkIn(Long id, AttendanceCheckInRequest request, CurrentUser currentUser) {
        ensureSiteStaff(currentUser);
        AttendanceRecordView existing = getAttendanceRecordById(id, currentUser);
        ensureEnrollmentConfirmed(existing.enrollmentId());
        if ("CHECKED_IN".equals(existing.attendanceStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该报名已完成签到，不能重复签到");
        }

        LocalDateTime now = LocalDateTime.now();
        int updatedRows = jdbcTemplate.update(
                """
                UPDATE attendance_record
                SET status = 'CHECKED_IN', checked_in_at = ?, checked_in_by = ?, remark = ?, updated_at = ?
                WHERE id = ?
                """,
                Timestamp.valueOf(now),
                currentUser.id(),
                emptyToNull(request.remark()),
                Timestamp.valueOf(now),
                id
        );
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "签到记录不存在");
        }

        writeOperationLog(currentUser.id(), id, "ATTENDANCE", "CHECK_IN", "执行签到");
        return getAttendanceRecordById(id, currentUser);
    }

    private void ensureEnrollmentConfirmed(Long enrollmentId) {
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM enrollment WHERE id = ?",
                String.class,
                enrollmentId
        );
        if (!"CONFIRMED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "仅已确认报名允许签到");
        }
    }

    private void writeOperationLog(Long operatorUserId, Long businessId, String businessType, String actionType, String detail) {
        jdbcTemplate.update(
                """
                INSERT INTO operation_log (
                    operator_user_id, business_type, business_id, action_type, action_result, action_detail, created_at
                ) VALUES (?, ?, ?, ?, 'SUCCESS', ?, ?)
                """,
                operatorUserId,
                businessType,
                businessId,
                actionType,
                detail,
                Timestamp.valueOf(LocalDateTime.now())
        );
    }

    private void ensureSiteStaff(CurrentUser currentUser) {
        if (currentUser.hasRole("SITE_STAFF")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅现场工作人员可操作签到管理");
    }

    private String normalizeLikeKeyword(String keyword) {
        String normalized = emptyToNull(keyword);
        return normalized == null ? null : "%" + normalized + "%";
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
