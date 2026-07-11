package com.hqtraining.backend.service;

import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.EnrollmentConfirmRequest;
import com.hqtraining.backend.dto.EnrollmentCreateRequest;
import com.hqtraining.backend.model.CourseOptionRecord;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.EnrollmentRecord;
import com.hqtraining.backend.model.StudentOptionRecord;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EnrollmentService {

    private static final RowMapper<EnrollmentRecord> ENROLLMENT_ROW_MAPPER = (rs, rowNum) -> new EnrollmentRecord(
            rs.getLong("id"),
            rs.getString("enrollment_no"),
            rs.getLong("course_id"),
            rs.getString("course_no"),
            rs.getString("course_name"),
            rs.getString("location"),
            rs.getTimestamp("start_time").toLocalDateTime(),
            rs.getBigDecimal("fee_amount"),
            rs.getLong("student_id"),
            rs.getString("student_no"),
            rs.getString("student_name"),
            rs.getString("company_name"),
            rs.getString("payment_type"),
            rs.getString("status"),
            rs.getObject("confirmed_by", Long.class),
            rs.getString("confirmed_by_name"),
            toLocalDateTime(rs.getTimestamp("confirmed_at")),
            rs.getString("reject_reason"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private static final RowMapper<CourseOptionRecord> COURSE_OPTION_ROW_MAPPER = (rs, rowNum) -> new CourseOptionRecord(
            rs.getLong("id"),
            rs.getString("course_no"),
            rs.getString("course_name"),
            rs.getString("location"),
            rs.getTimestamp("start_time").toLocalDateTime(),
            rs.getInt("quota"),
            rs.getBigDecimal("fee_amount"),
            rs.getString("status")
    );

    private static final RowMapper<StudentOptionRecord> STUDENT_OPTION_ROW_MAPPER = (rs, rowNum) -> new StudentOptionRecord(
            rs.getLong("id"),
            rs.getString("student_no"),
            rs.getString("full_name"),
            rs.getString("company_name"),
            rs.getString("phone"),
            rs.getString("email")
    );

    private final JdbcTemplate jdbcTemplate;

    public EnrollmentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<EnrollmentRecord> getEnrollments(
            int pageNum,
            int pageSize,
            String keyword,
            String status,
            Long courseId,
            Long studentId,
            CurrentUser currentUser
    ) {
        ensureEnrollmentReader(currentUser);
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        String likeKeyword = normalizeLikeKeyword(keyword);
        Long effectiveStudentId = currentUser.hasRole("STUDENT")
                ? getStudentProfileIdByUserId(currentUser.id())
                : studentId;

        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM enrollment e
                INNER JOIN course c ON e.course_id = c.id
                INNER JOIN student_profile s ON e.student_id = s.id
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                WHERE (? IS NULL OR e.status = ?)
                  AND (? IS NULL OR e.course_id = ?)
                  AND (? IS NULL OR e.student_id = ?)
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
                effectiveStudentId, effectiveStudentId,
                likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword
        );

        List<EnrollmentRecord> list = jdbcTemplate.query(
                """
                SELECT
                    e.id,
                    e.enrollment_no,
                    e.course_id,
                    c.course_no,
                    c.course_name,
                    c.location,
                    c.start_time,
                    c.fee_amount,
                    e.student_id,
                    s.student_no,
                    s.full_name AS student_name,
                    COALESCE(cc.company_name, '') AS company_name,
                    e.payment_type,
                    e.status,
                    e.confirmed_by,
                    COALESCE(u.display_name, '') AS confirmed_by_name,
                    e.confirmed_at,
                    e.reject_reason,
                    e.created_at,
                    e.updated_at
                FROM enrollment e
                INNER JOIN course c ON e.course_id = c.id
                INNER JOIN student_profile s ON e.student_id = s.id
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                LEFT JOIN user_account u ON e.confirmed_by = u.id
                WHERE (? IS NULL OR e.status = ?)
                  AND (? IS NULL OR e.course_id = ?)
                  AND (? IS NULL OR e.student_id = ?)
                  AND (
                    ? IS NULL
                    OR e.enrollment_no LIKE ?
                    OR c.course_no LIKE ?
                    OR c.course_name LIKE ?
                    OR s.student_no LIKE ?
                    OR s.full_name LIKE ?
                    OR COALESCE(cc.company_name, '') LIKE ?
                  )
                ORDER BY e.created_at DESC, e.id DESC
                LIMIT ? OFFSET ?
                """,
                ENROLLMENT_ROW_MAPPER,
                emptyToNull(status), emptyToNull(status),
                courseId, courseId,
                effectiveStudentId, effectiveStudentId,
                likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword,
                safePageSize, (safePageNum - 1) * safePageSize
        );

        return new PageResult<>(list, safePageNum, safePageSize, total == null ? 0 : total);
    }

    public EnrollmentRecord getEnrollmentById(Long id, CurrentUser currentUser) {
        ensureEnrollmentReader(currentUser);
        List<EnrollmentRecord> records = jdbcTemplate.query(
                """
                SELECT
                    e.id,
                    e.enrollment_no,
                    e.course_id,
                    c.course_no,
                    c.course_name,
                    c.location,
                    c.start_time,
                    c.fee_amount,
                    e.student_id,
                    s.student_no,
                    s.full_name AS student_name,
                    COALESCE(cc.company_name, '') AS company_name,
                    e.payment_type,
                    e.status,
                    e.confirmed_by,
                    COALESCE(u.display_name, '') AS confirmed_by_name,
                    e.confirmed_at,
                    e.reject_reason,
                    e.created_at,
                    e.updated_at
                FROM enrollment e
                INNER JOIN course c ON e.course_id = c.id
                INNER JOIN student_profile s ON e.student_id = s.id
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                LEFT JOIN user_account u ON e.confirmed_by = u.id
                WHERE e.id = ?
                """,
                ENROLLMENT_ROW_MAPPER,
                id
        );
        if (records.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "报名记录不存在");
        }
        EnrollmentRecord record = records.get(0);
        if (currentUser.hasRole("STUDENT")) {
            Long studentProfileId = getStudentProfileIdByUserId(currentUser.id());
            if (!studentProfileId.equals(record.studentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "学员仅可查看自己的报名记录");
            }
        }
        return record;
    }

    public EnrollmentRecord createEnrollment(EnrollmentCreateRequest request, CurrentUser currentUser) {
        ensureEnrollmentCreator(currentUser);
        ensureDemoStudentsReady();
        CourseOptionRecord course = getPublishedCourseOption(request.courseId());
        Long effectiveStudentId = request.studentId();
        if (currentUser.hasRole("STUDENT")) {
            effectiveStudentId = getStudentProfileIdByUserId(currentUser.id());
            if (!effectiveStudentId.equals(request.studentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "学员仅可为自己提交报名");
            }
        }
        final Long finalStudentId = effectiveStudentId;
        StudentOptionRecord student = getStudentOption(effectiveStudentId);
        validateCreateRequest(request.courseId(), effectiveStudentId, request.paymentType(), course, student);
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO enrollment (
                        enrollment_no, course_id, student_id, payment_type, status, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, 'PENDING', ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, generateEnrollmentNo());
            statement.setLong(2, request.courseId());
            statement.setLong(3, finalStudentId);
            statement.setString(4, request.paymentType().trim().toUpperCase());
            statement.setTimestamp(5, Timestamp.valueOf(now));
            statement.setTimestamp(6, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "报名创建失败");
        }

        writeOperationLog(currentUser.id(), "ENROLLMENT", key.longValue(), "CREATE", "新增报名记录");
        return getEnrollmentById(key.longValue(), currentUser);
    }

    public EnrollmentRecord confirmEnrollment(Long id, EnrollmentConfirmRequest request, CurrentUser currentUser) {
        ensureExecutor(currentUser);
        EnrollmentRecord existing = getEnrollmentById(id, currentUser);
        if (!"PENDING".equals(existing.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只有待审核报名可以执行审核");
        }

        boolean approved = Boolean.TRUE.equals(request.approved());
        LocalDateTime now = LocalDateTime.now();
        String nextStatus = approved ? "CONFIRMED" : "REJECTED";
        String rejectReason = approved ? null : emptyToNull(request.rejectReason());
        if (!approved && rejectReason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "驳回报名时必须填写驳回原因");
        }

        int updatedRows = jdbcTemplate.update(
                """
                UPDATE enrollment
                SET status = ?, confirmed_by = ?, confirmed_at = ?, reject_reason = ?, updated_at = ?
                WHERE id = ?
                """,
                nextStatus,
                currentUser.id(),
                Timestamp.valueOf(now),
                rejectReason,
                Timestamp.valueOf(now),
                id
        );
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "报名记录不存在");
        }

        if (approved) {
            initializeAttendanceRecord(existing, now);
            initializePaymentRecord(existing, now, currentUser.id());
        }

        writeOperationLog(
                currentUser.id(),
                "ENROLLMENT",
                id,
                approved ? "APPROVE" : "REJECT",
                approved ? "报名审核通过" : "报名审核驳回：" + rejectReason
        );
        return getEnrollmentById(id, currentUser);
    }

    public List<CourseOptionRecord> getCourseOptions(CurrentUser currentUser) {
        ensureEnrollmentReader(currentUser);
        return jdbcTemplate.query(
                """
                SELECT id, course_no, course_name, location, start_time, quota, fee_amount, status
                FROM course
                WHERE status = 'PUBLISHED'
                ORDER BY start_time ASC, id ASC
                """,
                COURSE_OPTION_ROW_MAPPER
        );
    }

    public List<StudentOptionRecord> getStudentOptions(CurrentUser currentUser) {
        ensureEnrollmentCreator(currentUser);
        ensureDemoStudentsReady();
        if (currentUser.hasRole("STUDENT")) {
            StudentOptionRecord student = getStudentOption(getStudentProfileIdByUserId(currentUser.id()));
            return student == null ? List.of() : List.of(student);
        }
        return jdbcTemplate.query(
                """
                SELECT
                    s.id,
                    s.student_no,
                    s.full_name,
                    COALESCE(cc.company_name, '') AS company_name,
                    COALESCE(s.phone, '') AS phone,
                    COALESCE(s.email, '') AS email
                FROM student_profile s
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                LEFT JOIN user_account ua ON s.user_id = ua.id
                WHERE s.user_id IS NULL OR ua.account_status = 'ACTIVE'
                ORDER BY s.full_name ASC, s.id ASC
                """,
                STUDENT_OPTION_ROW_MAPPER
        );
    }

    private void validateCreateRequest(
            Long courseId,
            Long studentId,
            String paymentTypeValue,
            CourseOptionRecord course,
            StudentOptionRecord student
    ) {
        String paymentType = paymentTypeValue.trim().toUpperCase();
        if (!"PERSONAL".equals(paymentType) && !"CORPORATE".equals(paymentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "付费类型仅支持 PERSONAL 或 CORPORATE");
        }
        if (course == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在或尚未发布");
        }
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学员不存在");
        }

        Integer existingCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM enrollment
                WHERE course_id = ?
                  AND student_id = ?
                """,
                Integer.class,
                courseId,
                studentId
        );
        if (existingCount != null && existingCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "同一学员不能重复报名同一课程");
        }

        Integer occupiedCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM enrollment
                WHERE course_id = ?
                  AND status IN ('PENDING', 'CONFIRMED')
                """,
                Integer.class,
                courseId
        );
        if (occupiedCount != null && occupiedCount >= course.quota()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "课程名额已满，无法继续报名");
        }
    }

    private CourseOptionRecord getPublishedCourseOption(Long courseId) {
        List<CourseOptionRecord> courses = jdbcTemplate.query(
                """
                SELECT id, course_no, course_name, location, start_time, quota, fee_amount, status
                FROM course
                WHERE id = ?
                  AND status = 'PUBLISHED'
                """,
                COURSE_OPTION_ROW_MAPPER,
                courseId
        );
        return courses.isEmpty() ? null : courses.get(0);
    }

    private StudentOptionRecord getStudentOption(Long studentId) {
        List<StudentOptionRecord> students = jdbcTemplate.query(
                """
                SELECT
                    s.id,
                    s.student_no,
                    s.full_name,
                    COALESCE(cc.company_name, '') AS company_name,
                    COALESCE(s.phone, '') AS phone,
                    COALESCE(s.email, '') AS email
                FROM student_profile s
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                LEFT JOIN user_account ua ON s.user_id = ua.id
                WHERE s.id = ?
                  AND (s.user_id IS NULL OR ua.account_status = 'ACTIVE')
                """,
                STUDENT_OPTION_ROW_MAPPER,
                studentId
        );
        return students.isEmpty() ? null : students.get(0);
    }

    private void initializeAttendanceRecord(EnrollmentRecord existing, LocalDateTime now) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM attendance_record WHERE enrollment_id = ?",
                Integer.class,
                existing.id()
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                """
                INSERT INTO attendance_record (
                    enrollment_id, course_id, student_id, status, created_at, updated_at
                ) VALUES (?, ?, ?, 'NOT_CHECKED_IN', ?, ?)
                """,
                existing.id(),
                existing.courseId(),
                existing.studentId(),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );
    }

    private void initializePaymentRecord(EnrollmentRecord existing, LocalDateTime now, Long operatorUserId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM payment_record WHERE enrollment_id = ?",
                Integer.class,
                existing.id()
        );
        if (count != null && count > 0) {
            return;
        }

        boolean corporate = "CORPORATE".equals(existing.paymentType());
        // 企业统付课程也需要计入应收与实收，方便后续收入报表口径保持一致。
        BigDecimal receivableAmount = existing.courseFeeAmount();
        BigDecimal paidAmount = corporate ? existing.courseFeeAmount() : BigDecimal.ZERO;
        String paymentMethod = corporate ? "CORPORATE" : null;
        String paymentStatus = corporate ? "CORPORATE_PAID" : "UNPAID";

        jdbcTemplate.update(
                """
                INSERT INTO payment_record (
                    enrollment_id, course_id, student_id, receivable_amount, paid_amount, payment_method,
                    payment_status, paid_at, handled_by, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                existing.id(),
                existing.courseId(),
                existing.studentId(),
                receivableAmount,
                paidAmount,
                paymentMethod,
                paymentStatus,
                corporate ? Timestamp.valueOf(now) : null,
                corporate ? operatorUserId : null,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );
    }

    private void ensureEnrollmentReader(CurrentUser currentUser) {
        if (currentUser.hasRole("EXECUTOR") || currentUser.hasRole("STUDENT")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权访问报名信息");
    }

    private void ensureEnrollmentCreator(CurrentUser currentUser) {
        if (currentUser.hasRole("EXECUTOR") || currentUser.hasRole("STUDENT")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权提交报名");
    }

    private void ensureExecutor(CurrentUser currentUser) {
        if (currentUser.hasRole("EXECUTOR")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅执行人可审核报名");
    }

    private Long getStudentProfileIdByUserId(Long userId) {
        List<Long> studentIds = jdbcTemplate.queryForList(
                "SELECT id FROM student_profile WHERE user_id = ?",
                Long.class,
                userId
        );
        if (studentIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "当前账号尚未绑定学员档案");
        }
        return studentIds.get(0);
    }

    private void ensureDemoStudentsReady() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM student_profile", Integer.class);
        if (count != null && count > 0) {
            return;
        }

        List<Long> studentUserIds = jdbcTemplate.queryForList(
                """
                SELECT id
                FROM user_account
                WHERE account_type = 'STUDENT'
                ORDER BY id ASC
                """,
                Long.class
        );

        for (Long userId : studentUserIds) {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM student_profile WHERE user_id = ?",
                    Integer.class,
                    userId
            );
            if (exists != null && exists > 0) {
                continue;
            }

            long nextId = nextStudentSerial();
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update(
                    """
                    INSERT INTO student_profile (
                        user_id, student_no, full_name, gender, company_id, job_title, education_level, tech_level,
                        phone, email, created_at, updated_at
                    )
                    SELECT
                        ua.id,
                        ?,
                        ua.display_name,
                        '男',
                        1,
                        '软件工程师',
                        '本科',
                        '中级',
                        ua.phone,
                        ua.email,
                        ?,
                        ?
                    FROM user_account ua
                    WHERE ua.id = ?
                    """,
                    "STU20260709%03d".formatted(nextId),
                    Timestamp.valueOf(now),
                    Timestamp.valueOf(now),
                    userId
            );
        }
    }

    private long nextStudentSerial() {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM student_profile", Long.class);
        return (maxId == null ? 0 : maxId) + 1;
    }

    private String generateEnrollmentNo() {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM enrollment", Long.class);
        long nextId = (maxId == null ? 0 : maxId) + 1;
        return "ENR20260709%03d".formatted(nextId);
    }

    private void writeOperationLog(Long operatorUserId, String businessType, Long businessId, String actionType, String detail) {
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
