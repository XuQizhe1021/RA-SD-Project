package com.hqtraining.backend.service;

import com.hqtraining.backend.dto.EvaluationSubmitRequest;
import com.hqtraining.backend.model.CourseEvaluationReport;
import com.hqtraining.backend.model.CourseEvaluationSummary;
import com.hqtraining.backend.model.CourseOptionRecord;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.EvaluationCandidateRecord;
import com.hqtraining.backend.model.EvaluationRecordView;
import com.hqtraining.backend.model.PendingEvaluationCourse;
import com.hqtraining.backend.model.ScoreDistributionItem;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationService {

    private static final RowMapper<PendingEvaluationCourse> PENDING_COURSE_ROW_MAPPER = (rs, rowNum) -> new PendingEvaluationCourse(
            rs.getLong("course_id"),
            rs.getLong("enrollment_id"),
            rs.getString("course_no"),
            rs.getString("course_name"),
            rs.getString("lecturer_name"),
            rs.getTimestamp("start_time").toLocalDateTime(),
            rs.getTimestamp("end_time").toLocalDateTime(),
            rs.getString("location")
    );

    private static final RowMapper<EvaluationRecordView> EVALUATION_ROW_MAPPER = (rs, rowNum) -> new EvaluationRecordView(
            rs.getLong("id"),
            rs.getLong("course_id"),
            rs.getLong("enrollment_id"),
            rs.getString("course_no"),
            rs.getString("course_name"),
            rs.getString("lecturer_name"),
            rs.getLong("student_id"),
            rs.getString("student_name"),
            rs.getString("company_name"),
            rs.getInt("rating"),
            rs.getString("comment_text"),
            rs.getString("source"),
            rs.getObject("proxy_staff_id", Long.class),
            rs.getString("proxy_staff_name"),
            rs.getObject("submitted_by", Long.class),
            rs.getString("submitted_by_name"),
            rs.getTimestamp("submitted_at").toLocalDateTime()
    );

    private static final RowMapper<EvaluationCandidateRecord> EVALUATION_CANDIDATE_ROW_MAPPER = (rs, rowNum) -> new EvaluationCandidateRecord(
            rs.getLong("course_id"),
            rs.getLong("enrollment_id"),
            rs.getLong("student_id"),
            rs.getString("student_no"),
            rs.getString("student_name"),
            rs.getString("company_name"),
            rs.getTimestamp("checked_in_at").toLocalDateTime(),
            rs.getString("evaluation_status"),
            rs.getString("evaluation_source"),
            toLocalDateTime(rs.getTimestamp("submitted_at"))
    );

    private static final RowMapper<CourseEvaluationSummary> COURSE_SUMMARY_ROW_MAPPER = (rs, rowNum) -> new CourseEvaluationSummary(
            rs.getLong("course_id"),
            rs.getString("course_no"),
            rs.getString("course_name"),
            rs.getString("lecturer_name"),
            rs.getTimestamp("start_time").toLocalDateTime(),
            rs.getTimestamp("end_time").toLocalDateTime(),
            rs.getInt("should_evaluate_count"),
            rs.getInt("evaluated_count"),
            rs.getBigDecimal("average_rating")
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

    private final JdbcTemplate jdbcTemplate;

    public EvaluationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PendingEvaluationCourse> getPendingCourses(CurrentUser currentUser) {
        ensureStudent(currentUser);
        Long studentId = getStudentProfileIdByUserId(currentUser.id());
        return jdbcTemplate.query(
                """
                SELECT
                    c.id AS course_id,
                    e.id AS enrollment_id,
                    c.course_no,
                    c.course_name,
                    COALESCE(lp.full_name, '未分配讲师') AS lecturer_name,
                    c.start_time,
                    c.end_time,
                    c.location
                FROM enrollment e
                INNER JOIN course c ON e.course_id = c.id
                INNER JOIN attendance_record ar ON ar.enrollment_id = e.id
                LEFT JOIN lecturer_profile lp ON c.lecturer_id = lp.id
                LEFT JOIN course_evaluation ce ON ce.course_id = e.course_id AND ce.student_id = e.student_id
                WHERE e.student_id = ?
                  AND e.status = 'CONFIRMED'
                  AND ar.status = 'CHECKED_IN'
                  AND ce.id IS NULL
                  AND (c.status = 'FINISHED' OR c.end_time <= NOW())
                ORDER BY c.end_time DESC, c.id DESC
                """,
                PENDING_COURSE_ROW_MAPPER,
                studentId
        );
    }

    public List<EvaluationRecordView> getMyEvaluations(CurrentUser currentUser) {
        ensureStudent(currentUser);
        Long studentId = getStudentProfileIdByUserId(currentUser.id());
        return jdbcTemplate.query(
                baseEvaluationSelect() + """
                WHERE ce.student_id = ?
                ORDER BY ce.submitted_at DESC, ce.id DESC
                """,
                EVALUATION_ROW_MAPPER,
                studentId
        );
    }

    public List<CourseOptionRecord> getProxyCourseOptions(CurrentUser currentUser) {
        ensureSiteStaff(currentUser);
        return jdbcTemplate.query(
                """
                SELECT id, course_no, course_name, location, start_time, quota, fee_amount, status
                FROM course
                WHERE status = 'FINISHED' OR end_time <= NOW()
                ORDER BY end_time DESC, id DESC
                """,
                COURSE_OPTION_ROW_MAPPER
        );
    }

    public List<EvaluationCandidateRecord> getProxyCandidates(Long courseId, CurrentUser currentUser) {
        ensureSiteStaff(currentUser);
        return jdbcTemplate.query(
                """
                SELECT
                    c.id AS course_id,
                    e.id AS enrollment_id,
                    s.id AS student_id,
                    s.student_no,
                    s.full_name AS student_name,
                    COALESCE(cc.company_name, '') AS company_name,
                    ar.checked_in_at,
                    CASE WHEN ce.id IS NULL THEN 'PENDING' ELSE 'SUBMITTED' END AS evaluation_status,
                    ce.source AS evaluation_source,
                    ce.submitted_at
                FROM enrollment e
                INNER JOIN course c ON e.course_id = c.id
                INNER JOIN student_profile s ON e.student_id = s.id
                INNER JOIN attendance_record ar ON ar.enrollment_id = e.id
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                LEFT JOIN course_evaluation ce ON ce.course_id = e.course_id AND ce.student_id = e.student_id
                WHERE c.id = ?
                  AND e.status = 'CONFIRMED'
                  AND ar.status = 'CHECKED_IN'
                  AND (c.status = 'FINISHED' OR c.end_time <= NOW())
                ORDER BY
                    CASE WHEN ce.id IS NULL THEN 0 ELSE 1 END ASC,
                    ar.checked_in_at ASC,
                    s.id ASC
                """,
                EVALUATION_CANDIDATE_ROW_MAPPER,
                courseId
        );
    }

    public List<CourseEvaluationSummary> getCourseEvaluationSummaries(
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            Boolean hasEvaluation,
            CurrentUser currentUser
    ) {
        ensureEvaluationReader(currentUser);
        String likeKeyword = normalizeLikeKeyword(keyword);
        Timestamp startAt = toStartTimestamp(startDate);
        Timestamp endExclusive = toEndExclusiveTimestamp(endDate);

        // 使用聚合子查询避免报名、评价联表后出现重复计数。
        return jdbcTemplate.query(
                """
                SELECT
                    c.id AS course_id,
                    c.course_no,
                    c.course_name,
                    COALESCE(lp.full_name, '未分配讲师') AS lecturer_name,
                    c.start_time,
                    c.end_time,
                    COALESCE(stats.should_evaluate_count, 0) AS should_evaluate_count,
                    COALESCE(stats.evaluated_count, 0) AS evaluated_count,
                    stats.average_rating
                FROM course c
                LEFT JOIN lecturer_profile lp ON c.lecturer_id = lp.id
                LEFT JOIN (
                    SELECT
                        e.course_id,
                        COUNT(DISTINCT CASE
                            WHEN e.status = 'CONFIRMED' AND ar.status = 'CHECKED_IN' THEN e.student_id
                            ELSE NULL
                        END) AS should_evaluate_count,
                        COUNT(DISTINCT ce.student_id) AS evaluated_count,
                        ROUND(AVG(ce.rating), 1) AS average_rating
                    FROM enrollment e
                    LEFT JOIN attendance_record ar ON ar.enrollment_id = e.id
                    LEFT JOIN course_evaluation ce ON ce.course_id = e.course_id AND ce.student_id = e.student_id
                    GROUP BY e.course_id
                ) stats ON stats.course_id = c.id
                WHERE (c.status = 'FINISHED' OR c.end_time <= NOW() OR COALESCE(stats.evaluated_count, 0) > 0)
                  AND (? IS NULL OR c.start_time >= ?)
                  AND (? IS NULL OR c.start_time < ?)
                  AND (
                    ? IS NULL
                    OR c.course_no LIKE ?
                    OR c.course_name LIKE ?
                    OR COALESCE(lp.full_name, '') LIKE ?
                  )
                  AND (
                    ? IS NULL
                    OR (? = TRUE AND COALESCE(stats.evaluated_count, 0) > 0)
                    OR (? = FALSE AND COALESCE(stats.evaluated_count, 0) = 0)
                  )
                ORDER BY c.end_time DESC, c.id DESC
                """,
                COURSE_SUMMARY_ROW_MAPPER,
                startAt, startAt,
                endExclusive, endExclusive,
                likeKeyword, likeKeyword, likeKeyword, likeKeyword,
                hasEvaluation, hasEvaluation, hasEvaluation
        );
    }

    public CourseEvaluationReport getCourseEvaluationReport(Long courseId, CurrentUser currentUser) {
        ensureEvaluationReader(currentUser);
        List<CourseEvaluationSummary> summaries = jdbcTemplate.query(
                """
                SELECT
                    c.id AS course_id,
                    c.course_no,
                    c.course_name,
                    COALESCE(lp.full_name, '未分配讲师') AS lecturer_name,
                    c.start_time,
                    c.end_time,
                    COUNT(DISTINCT CASE
                        WHEN e.status = 'CONFIRMED' AND ar.status = 'CHECKED_IN' THEN e.student_id
                        ELSE NULL
                    END) AS should_evaluate_count,
                    COUNT(DISTINCT ce.student_id) AS evaluated_count,
                    ROUND(AVG(ce.rating), 1) AS average_rating
                FROM course c
                LEFT JOIN lecturer_profile lp ON c.lecturer_id = lp.id
                LEFT JOIN enrollment e ON e.course_id = c.id
                LEFT JOIN attendance_record ar ON ar.enrollment_id = e.id
                LEFT JOIN course_evaluation ce ON ce.course_id = c.id AND ce.student_id = e.student_id
                WHERE c.id = ?
                GROUP BY c.id, c.course_no, c.course_name, lp.full_name, c.start_time, c.end_time
                """,
                COURSE_SUMMARY_ROW_MAPPER,
                courseId
        );
        if (summaries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在");
        }

        CourseEvaluationSummary summary = summaries.get(0);
        String location = jdbcTemplate.queryForObject(
                "SELECT location FROM course WHERE id = ?",
                String.class,
                courseId
        );
        List<EvaluationRecordView> details = jdbcTemplate.query(
                baseEvaluationSelect() + """
                WHERE ce.course_id = ?
                ORDER BY ce.submitted_at DESC, ce.id DESC
                """,
                EVALUATION_ROW_MAPPER,
                courseId
        );

        List<ScoreDistributionItem> scoreDistribution = buildScoreDistribution(details);
        BigDecimal participationRate = calculateParticipationRate(summary.evaluatedCount(), summary.shouldEvaluateCount());

        return new CourseEvaluationReport(
                summary.courseId(),
                summary.courseNo(),
                summary.courseName(),
                summary.lecturerName(),
                summary.startTime(),
                summary.endTime(),
                location,
                summary.shouldEvaluateCount(),
                summary.evaluatedCount(),
                participationRate,
                summary.averageRating(),
                scoreDistribution,
                details
        );
    }

    public EvaluationRecordView submitEvaluation(EvaluationSubmitRequest request, CurrentUser currentUser) {
        boolean studentSelfSubmit = currentUser.hasRole("STUDENT");
        if (!studentSelfSubmit && !currentUser.hasRole("SITE_STAFF")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权提交评价");
        }

        Long effectiveStudentId = studentSelfSubmit
                ? getStudentProfileIdByUserId(currentUser.id())
                : request.studentId();
        if (!studentSelfSubmit && effectiveStudentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "代录评价时必须指定学员");
        }

        EnrollmentSnapshot enrollment = getEnrollmentSnapshot(request.enrollmentId());
        if (!enrollment.courseId().equals(request.courseId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "报名记录与课程不匹配");
        }
        if (!enrollment.studentId().equals(effectiveStudentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "报名记录与学员不匹配");
        }
        if (!"CONFIRMED".equals(enrollment.enrollmentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "仅已确认报名允许提交评价");
        }
        if (!"CHECKED_IN".equals(enrollment.attendanceStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "仅已签到学员允许提交评价");
        }
        if (!"FINISHED".equals(enrollment.courseStatus()) && enrollment.endTime().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "课程尚未结束，暂不能提交评价");
        }
        if (hasExistingEvaluation(request.courseId(), effectiveStudentId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该学员已评价过该课程");
        }

        LocalDateTime now = LocalDateTime.now();
        String source = studentSelfSubmit ? "STUDENT" : "STAFF_PROXY";
        Long proxyStaffId = studentSelfSubmit ? null : currentUser.id();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO course_evaluation (
                        course_id, student_id, enrollment_id, rating, comment_text, source,
                        proxy_staff_id, submitted_by, submitted_at, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, request.courseId());
            statement.setLong(2, effectiveStudentId);
            statement.setLong(3, request.enrollmentId());
            statement.setInt(4, request.rating());
            statement.setString(5, emptyToNull(request.commentText()));
            statement.setString(6, source);
            statement.setObject(7, proxyStaffId);
            statement.setLong(8, currentUser.id());
            statement.setTimestamp(9, Timestamp.valueOf(now));
            statement.setTimestamp(10, Timestamp.valueOf(now));
            statement.setTimestamp(11, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "评价提交失败");
        }

        writeOperationLog(
                currentUser.id(),
                key.longValue(),
                "EVALUATION",
                "SUBMIT",
                studentSelfSubmit ? "学员提交课程评价" : "现场工作人员代录课程评价"
        );
        return getEvaluationById(key.longValue(), currentUser);
    }

    private EvaluationRecordView getEvaluationById(Long id, CurrentUser currentUser) {
        List<EvaluationRecordView> list = jdbcTemplate.query(
                baseEvaluationSelect() + """
                WHERE ce.id = ?
                """,
                EVALUATION_ROW_MAPPER,
                id
        );
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评价记录不存在");
        }
        EvaluationRecordView record = list.get(0);
        if (currentUser.hasRole("STUDENT")) {
            Long studentId = getStudentProfileIdByUserId(currentUser.id());
            if (!studentId.equals(record.studentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "学员仅可查看自己的评价记录");
            }
        }
        return record;
    }

    private EnrollmentSnapshot getEnrollmentSnapshot(Long enrollmentId) {
        List<EnrollmentSnapshot> snapshots = jdbcTemplate.query(
                """
                SELECT
                    e.course_id,
                    e.student_id,
                    e.status AS enrollment_status,
                    c.status AS course_status,
                    c.end_time,
                    COALESCE(ar.status, 'NOT_CHECKED_IN') AS attendance_status
                FROM enrollment e
                INNER JOIN course c ON e.course_id = c.id
                LEFT JOIN attendance_record ar ON ar.enrollment_id = e.id
                WHERE e.id = ?
                """,
                (rs, rowNum) -> new EnrollmentSnapshot(
                        rs.getLong("course_id"),
                        rs.getLong("student_id"),
                        rs.getString("enrollment_status"),
                        rs.getString("course_status"),
                        rs.getTimestamp("end_time").toLocalDateTime(),
                        rs.getString("attendance_status")
                ),
                enrollmentId
        );
        if (snapshots.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "报名记录不存在");
        }
        return snapshots.get(0);
    }

    private boolean hasExistingEvaluation(Long courseId, Long studentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM course_evaluation WHERE course_id = ? AND student_id = ?",
                Integer.class,
                courseId,
                studentId
        );
        return count != null && count > 0;
    }

    private List<ScoreDistributionItem> buildScoreDistribution(List<EvaluationRecordView> details) {
        List<ScoreDistributionItem> items = new ArrayList<>();
        for (int score = 5; score >= 1; score--) {
            int count = 0;
            for (EvaluationRecordView detail : details) {
                if (detail.rating() != null && detail.rating() == score) {
                    count++;
                }
            }
            items.add(new ScoreDistributionItem(score, count));
        }
        return items;
    }

    private BigDecimal calculateParticipationRate(int evaluatedCount, int shouldEvaluateCount) {
        if (shouldEvaluateCount <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(evaluatedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(shouldEvaluateCount), 1, RoundingMode.HALF_UP);
    }

    private String baseEvaluationSelect() {
        return """
                SELECT
                    ce.id,
                    ce.course_id,
                    ce.enrollment_id,
                    c.course_no,
                    c.course_name,
                    COALESCE(lp.full_name, '未分配讲师') AS lecturer_name,
                    ce.student_id,
                    s.full_name AS student_name,
                    COALESCE(cc.company_name, '') AS company_name,
                    ce.rating,
                    COALESCE(ce.comment_text, '') AS comment_text,
                    ce.source,
                    ce.proxy_staff_id,
                    COALESCE(proxy_user.display_name, '') AS proxy_staff_name,
                    ce.submitted_by,
                    COALESCE(submitter.display_name, '') AS submitted_by_name,
                    ce.submitted_at
                FROM course_evaluation ce
                INNER JOIN course c ON ce.course_id = c.id
                INNER JOIN student_profile s ON ce.student_id = s.id
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                LEFT JOIN lecturer_profile lp ON c.lecturer_id = lp.id
                LEFT JOIN user_account proxy_user ON ce.proxy_staff_id = proxy_user.id
                LEFT JOIN user_account submitter ON ce.submitted_by = submitter.id
                """;
    }

    private void ensureStudent(CurrentUser currentUser) {
        if (currentUser.hasRole("STUDENT")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅学员可访问待评价列表");
    }

    private void ensureSiteStaff(CurrentUser currentUser) {
        if (currentUser.hasRole("SITE_STAFF")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅现场工作人员可访问代录评价");
    }

    private void ensureEvaluationReader(CurrentUser currentUser) {
        if (currentUser.hasRole("MANAGER") || currentUser.hasRole("EXECUTOR")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权查看评价汇总");
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

    private Timestamp toStartTimestamp(LocalDate date) {
        return date == null ? null : Timestamp.valueOf(date.atStartOfDay());
    }

    private Timestamp toEndExclusiveTimestamp(LocalDate date) {
        return date == null ? null : Timestamp.valueOf(date.plusDays(1).atStartOfDay());
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record EnrollmentSnapshot(
            Long courseId,
            Long studentId,
            String enrollmentStatus,
            String courseStatus,
            LocalDateTime endTime,
            String attendanceStatus
    ) {
    }
}
