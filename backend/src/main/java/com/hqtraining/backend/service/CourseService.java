package com.hqtraining.backend.service;

import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.CourseSaveRequest;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.CourseRecord;
import com.hqtraining.backend.model.LecturerRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseService {

    private static final RowMapper<CourseRecord> COURSE_ROW_MAPPER = (rs, rowNum) -> new CourseRecord(
            rs.getLong("id"),
            rs.getString("course_no"),
            rs.getObject("application_id", Long.class),
            rs.getString("course_name"),
            rs.getObject("lecturer_id", Long.class),
            rs.getString("lecturer_name"),
            rs.getLong("executor_user_id"),
            rs.getString("executor_name"),
            rs.getTimestamp("start_time").toLocalDateTime(),
            rs.getTimestamp("end_time").toLocalDateTime(),
            rs.getString("location"),
            rs.getInt("quota"),
            rs.getBigDecimal("fee_amount"),
            rs.getString("status"),
            rs.getString("source_type"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;
    private final LecturerService lecturerService;

    public CourseService(JdbcTemplate jdbcTemplate, LecturerService lecturerService) {
        this.jdbcTemplate = jdbcTemplate;
        this.lecturerService = lecturerService;
    }

    public PageResult<CourseRecord> getCourses(
            int pageNum,
            int pageSize,
            String keyword,
            String status,
            Long lecturerId,
            CurrentUser currentUser
    ) {
        ensureReadable(currentUser);
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        String likeKeyword = normalizeLikeKeyword(keyword);

        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM course c
                LEFT JOIN lecturer_profile l ON c.lecturer_id = l.id
                WHERE (? IS NULL OR c.status = ?)
                  AND (? IS NULL OR c.lecturer_id = ?)
                  AND (
                    ? IS NULL
                    OR c.course_no LIKE ?
                    OR c.course_name LIKE ?
                    OR c.location LIKE ?
                    OR COALESCE(l.full_name, '') LIKE ?
                  )
                """,
                Long.class,
                emptyToNull(status), emptyToNull(status),
                lecturerId, lecturerId,
                likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword
        );

        List<CourseRecord> list = jdbcTemplate.query(
                """
                SELECT
                    c.id,
                    c.course_no,
                    c.application_id,
                    c.course_name,
                    c.lecturer_id,
                    COALESCE(l.full_name, '') AS lecturer_name,
                    c.executor_user_id,
                    COALESCE(u.display_name, '') AS executor_name,
                    c.start_time,
                    c.end_time,
                    c.location,
                    c.quota,
                    c.fee_amount,
                    c.status,
                    c.source_type,
                    c.created_at,
                    c.updated_at
                FROM course c
                LEFT JOIN lecturer_profile l ON c.lecturer_id = l.id
                LEFT JOIN user_account u ON c.executor_user_id = u.id
                WHERE (? IS NULL OR c.status = ?)
                  AND (? IS NULL OR c.lecturer_id = ?)
                  AND (
                    ? IS NULL
                    OR c.course_no LIKE ?
                    OR c.course_name LIKE ?
                    OR c.location LIKE ?
                    OR COALESCE(l.full_name, '') LIKE ?
                  )
                ORDER BY c.start_time ASC, c.id ASC
                LIMIT ? OFFSET ?
                """,
                COURSE_ROW_MAPPER,
                emptyToNull(status), emptyToNull(status),
                lecturerId, lecturerId,
                likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword,
                safePageSize, (safePageNum - 1) * safePageSize
        );

        return new PageResult<>(list, safePageNum, safePageSize, total == null ? 0 : total);
    }

    public CourseRecord getCourseById(Long id, CurrentUser currentUser) {
        ensureReadable(currentUser);
        List<CourseRecord> courses = jdbcTemplate.query(
                """
                SELECT
                    c.id,
                    c.course_no,
                    c.application_id,
                    c.course_name,
                    c.lecturer_id,
                    COALESCE(l.full_name, '') AS lecturer_name,
                    c.executor_user_id,
                    COALESCE(u.display_name, '') AS executor_name,
                    c.start_time,
                    c.end_time,
                    c.location,
                    c.quota,
                    c.fee_amount,
                    c.status,
                    c.source_type,
                    c.created_at,
                    c.updated_at
                FROM course c
                LEFT JOIN lecturer_profile l ON c.lecturer_id = l.id
                LEFT JOIN user_account u ON c.executor_user_id = u.id
                WHERE c.id = ?
                """,
                COURSE_ROW_MAPPER,
                id
        );
        if (courses.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在");
        }
        return courses.get(0);
    }

    public CourseRecord createCourse(CourseSaveRequest request, CurrentUser currentUser) {
        ensureExecutor(currentUser);
        validateCourseRequest(request);
        Long applicationId = resolveApplicationId(request.applicationId());
        LecturerRecord lecturer = resolveLecturer(request.lecturerId());
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO course (
                        course_no, application_id, course_name, lecturer_id, executor_user_id, start_time, end_time, location,
                        quota, fee_amount, status, source_type, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', 'SYSTEM', ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, generateCourseNo());
            if (applicationId == null) {
                statement.setObject(2, null);
            } else {
                statement.setLong(2, applicationId);
            }
            statement.setString(3, request.courseName().trim());
            if (lecturer == null) {
                statement.setObject(4, null);
            } else {
                statement.setLong(4, lecturer.id());
            }
            statement.setLong(5, currentUser.id());
            statement.setTimestamp(6, Timestamp.valueOf(request.startTime()));
            statement.setTimestamp(7, Timestamp.valueOf(request.endTime()));
            statement.setString(8, request.location().trim());
            statement.setInt(9, request.quota());
            statement.setBigDecimal(10, request.feeAmount() == null ? BigDecimal.ZERO : request.feeAmount());
            statement.setTimestamp(11, Timestamp.valueOf(now));
            statement.setTimestamp(12, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "课程创建失败");
        }
        return getCourseById(key.longValue(), currentUser);
    }

    public CourseRecord updateCourse(Long id, CourseSaveRequest request, CurrentUser currentUser) {
        ensureExecutor(currentUser);
        validateCourseRequest(request);
        CourseRecord existing = getCourseById(id, currentUser);
        Long applicationId = resolveApplicationId(request.applicationId());
        LecturerRecord lecturer = resolveLecturer(request.lecturerId());

        int updatedRows = jdbcTemplate.update(
                """
                UPDATE course
                SET application_id = ?, course_name = ?, lecturer_id = ?, start_time = ?, end_time = ?, location = ?, quota = ?, fee_amount = ?, updated_at = ?
                WHERE id = ?
                """,
                applicationId,
                request.courseName().trim(),
                lecturer == null ? null : lecturer.id(),
                Timestamp.valueOf(request.startTime()),
                Timestamp.valueOf(request.endTime()),
                request.location().trim(),
                request.quota(),
                request.feeAmount() == null ? BigDecimal.ZERO : request.feeAmount(),
                Timestamp.valueOf(LocalDateTime.now()),
                existing.id()
        );
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在");
        }
        return getCourseById(id, currentUser);
    }

    public CourseRecord publishCourse(Long id, CurrentUser currentUser) {
        ensureExecutor(currentUser);
        CourseRecord existing = getCourseById(id, currentUser);
        if (!"DRAFT".equals(existing.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只有草稿状态的课程可以发布");
        }
        int updatedRows = jdbcTemplate.update(
                """
                UPDATE course
                SET status = 'PUBLISHED', updated_at = ?
                WHERE id = ?
                """,
                Timestamp.valueOf(LocalDateTime.now()),
                id
        );
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在");
        }
        return getCourseById(id, currentUser);
    }

    private void ensureReadable(CurrentUser currentUser) {
        if (currentUser.hasRole("MANAGER") || currentUser.hasRole("EXECUTOR")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权查看课程信息");
    }

    private void ensureExecutor(CurrentUser currentUser) {
        if (currentUser.hasRole("EXECUTOR")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅执行人可维护课程信息");
    }

    private void validateCourseRequest(CourseSaveRequest request) {
        if (request.endTime().isBefore(request.startTime()) || request.endTime().isEqual(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
    }

    private LecturerRecord resolveLecturer(Long lecturerId) {
        if (lecturerId == null) {
            return null;
        }
        LecturerRecord lecturer = lecturerService.getLecturerById(lecturerId);
        if (!"ACTIVE".equals(lecturer.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只能选择启用中的讲师");
        }
        return lecturer;
    }

    private Long resolveApplicationId(Long applicationId) {
        if (applicationId == null) {
            return null;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM training_application WHERE id = ?",
                Integer.class,
                applicationId
        );
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "关联的培训申请不存在");
        }
        return applicationId;
    }

    private String generateCourseNo() {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM course", Long.class);
        long nextId = (maxId == null ? 0 : maxId) + 1;
        return "CRS20260708%03d".formatted(nextId);
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
}
