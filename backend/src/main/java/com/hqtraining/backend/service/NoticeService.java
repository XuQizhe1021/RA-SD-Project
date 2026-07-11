package com.hqtraining.backend.service;

import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.NoticeSaveRequest;
import com.hqtraining.backend.model.CourseNoticeRecord;
import com.hqtraining.backend.model.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeService {

    private static final RowMapper<CourseNoticeRecord> NOTICE_ROW_MAPPER = (rs, rowNum) -> new CourseNoticeRecord(
            rs.getLong("id"),
            rs.getLong("course_id"),
            rs.getString("course_no"),
            rs.getString("course_name"),
            rs.getString("title"),
            rs.getString("content"),
            toLocalDateTime(rs.getTimestamp("registration_start_at")),
            toLocalDateTime(rs.getTimestamp("registration_end_at")),
            rs.getString("status"),
            toLocalDateTime(rs.getTimestamp("published_at")),
            rs.getBoolean("external_publish_flag"),
            rs.getLong("created_by"),
            rs.getString("created_by_name"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public NoticeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<CourseNoticeRecord> getNotices(
            int pageNum,
            int pageSize,
            String keyword,
            String status,
            Long courseId,
            CurrentUser currentUser
    ) {
        ensureReadable(currentUser);
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        String likeKeyword = normalizeLikeKeyword(keyword);
        boolean studentView = currentUser.hasRole("STUDENT");

        String visibilityClause = studentView ? "AND n.status = 'PUBLISHED'" : "";
        Object[] countArgs = studentView
                ? new Object[] {
                emptyToNull(status), emptyToNull(status),
                courseId, courseId,
                likeKeyword, likeKeyword, likeKeyword, likeKeyword
        }
                : new Object[] {
                emptyToNull(status), emptyToNull(status),
                courseId, courseId,
                likeKeyword, likeKeyword, likeKeyword, likeKeyword
        };

        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM course_notice n
                INNER JOIN course c ON n.course_id = c.id
                WHERE (? IS NULL OR n.status = ?)
                  AND (? IS NULL OR n.course_id = ?)
                  AND (
                    ? IS NULL
                    OR c.course_name LIKE ?
                    OR c.course_no LIKE ?
                    OR n.title LIKE ?
                  )
                """
                        + "\n" + visibilityClause,
                Long.class,
                countArgs
        );

        List<CourseNoticeRecord> list = jdbcTemplate.query(
                """
                SELECT
                    n.id,
                    n.course_id,
                    c.course_no,
                    c.course_name,
                    n.title,
                    n.content,
                    n.registration_start_at,
                    n.registration_end_at,
                    n.status,
                    n.published_at,
                    n.external_publish_flag,
                    n.created_by,
                    COALESCE(u.display_name, '') AS created_by_name,
                    n.created_at,
                    n.updated_at
                FROM course_notice n
                INNER JOIN course c ON n.course_id = c.id
                LEFT JOIN user_account u ON n.created_by = u.id
                WHERE (? IS NULL OR n.status = ?)
                  AND (? IS NULL OR n.course_id = ?)
                  AND (
                    ? IS NULL
                    OR c.course_name LIKE ?
                    OR c.course_no LIKE ?
                    OR n.title LIKE ?
                  )
                """
                        + "\n" + visibilityClause + """
                ORDER BY COALESCE(n.published_at, n.updated_at) DESC, n.id DESC
                LIMIT ? OFFSET ?
                """,
                NOTICE_ROW_MAPPER,
                emptyToNull(status), emptyToNull(status),
                courseId, courseId,
                likeKeyword, likeKeyword, likeKeyword, likeKeyword,
                safePageSize, (safePageNum - 1) * safePageSize
        );

        return new PageResult<>(list, safePageNum, safePageSize, total == null ? 0 : total);
    }

    public CourseNoticeRecord getNoticeById(Long id, CurrentUser currentUser) {
        ensureReadable(currentUser);
        List<CourseNoticeRecord> list = jdbcTemplate.query(
                """
                SELECT
                    n.id,
                    n.course_id,
                    c.course_no,
                    c.course_name,
                    n.title,
                    n.content,
                    n.registration_start_at,
                    n.registration_end_at,
                    n.status,
                    n.published_at,
                    n.external_publish_flag,
                    n.created_by,
                    COALESCE(u.display_name, '') AS created_by_name,
                    n.created_at,
                    n.updated_at
                FROM course_notice n
                INNER JOIN course c ON n.course_id = c.id
                LEFT JOIN user_account u ON n.created_by = u.id
                WHERE n.id = ?
                """,
                NOTICE_ROW_MAPPER,
                id
        );
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "培训通知不存在");
        }
        CourseNoticeRecord record = list.get(0);
        if (currentUser.hasRole("STUDENT") && !"PUBLISHED".equals(record.status())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "学员仅可查看已发布通知");
        }
        return record;
    }

    public CourseNoticeRecord createNotice(NoticeSaveRequest request, CurrentUser currentUser) {
        ensureExecutor(currentUser);
        validateNoticeRequest(request);
        ensureCourseReady(request.courseId());
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO course_notice (
                        course_id, title, content, registration_start_at, registration_end_at,
                        status, published_at, external_publish_flag, created_by, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, 'DRAFT', NULL, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, request.courseId());
            statement.setString(2, request.title().trim());
            statement.setString(3, request.content().trim());
            statement.setTimestamp(4, toTimestamp(request.registrationStartAt()));
            statement.setTimestamp(5, toTimestamp(request.registrationEndAt()));
            statement.setBoolean(6, Boolean.TRUE.equals(request.externalPublishFlag()));
            statement.setLong(7, currentUser.id());
            statement.setTimestamp(8, Timestamp.valueOf(now));
            statement.setTimestamp(9, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "培训通知创建失败");
        }
        writeOperationLog(currentUser.id(), "NOTICE", key.longValue(), "CREATE", "新增培训通知");
        return getNoticeById(key.longValue(), currentUser);
    }

    public CourseNoticeRecord updateNotice(Long id, NoticeSaveRequest request, CurrentUser currentUser) {
        ensureExecutor(currentUser);
        validateNoticeRequest(request);
        CourseNoticeRecord existing = getNoticeById(id, currentUser);
        if ("PUBLISHED".equals(existing.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已发布通知请先撤回后再修改");
        }
        ensureCourseReady(request.courseId());

        int updatedRows = jdbcTemplate.update(
                """
                UPDATE course_notice
                SET course_id = ?, title = ?, content = ?, registration_start_at = ?, registration_end_at = ?,
                    external_publish_flag = ?, updated_at = ?
                WHERE id = ?
                """,
                request.courseId(),
                request.title().trim(),
                request.content().trim(),
                toTimestamp(request.registrationStartAt()),
                toTimestamp(request.registrationEndAt()),
                Boolean.TRUE.equals(request.externalPublishFlag()),
                Timestamp.valueOf(LocalDateTime.now()),
                id
        );
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "培训通知不存在");
        }
        return getNoticeById(id, currentUser);
    }

    public CourseNoticeRecord publishNotice(Long id, CurrentUser currentUser) {
        ensureExecutor(currentUser);
        CourseNoticeRecord existing = getNoticeById(id, currentUser);
        if (!"DRAFT".equals(existing.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只有草稿通知可以发布");
        }
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                UPDATE course_notice
                SET status = 'PUBLISHED', published_at = ?, updated_at = ?
                WHERE id = ?
                """,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                id
        );
        writeOperationLog(currentUser.id(), "NOTICE", id, "PUBLISH", "发布培训通知");
        return getNoticeById(id, currentUser);
    }

    public CourseNoticeRecord revokeNotice(Long id, CurrentUser currentUser) {
        ensureExecutor(currentUser);
        CourseNoticeRecord existing = getNoticeById(id, currentUser);
        if (!"PUBLISHED".equals(existing.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只有已发布通知可以撤回");
        }
        jdbcTemplate.update(
                """
                UPDATE course_notice
                SET status = 'DRAFT', published_at = NULL, updated_at = ?
                WHERE id = ?
                """,
                Timestamp.valueOf(LocalDateTime.now()),
                id
        );
        writeOperationLog(currentUser.id(), "NOTICE", id, "REVOKE", "撤回培训通知");
        return getNoticeById(id, currentUser);
    }

    private void ensureCourseReady(Long courseId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM course WHERE id = ? AND status IN ('PUBLISHED', 'ONGOING', 'FINISHED')",
                Integer.class,
                courseId
        );
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅已发布或已执行课程允许发布通知");
        }
    }

    private void validateNoticeRequest(NoticeSaveRequest request) {
        if (request.registrationStartAt() != null && request.registrationEndAt() != null
                && !request.registrationEndAt().isAfter(request.registrationStartAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "报名截止时间必须晚于报名开始时间");
        }
    }

    private void ensureReadable(CurrentUser currentUser) {
        if (currentUser.hasRole("EXECUTOR") || currentUser.hasRole("STUDENT")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权查看培训通知");
    }

    private void ensureExecutor(CurrentUser currentUser) {
        if (currentUser.hasRole("EXECUTOR")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅执行人可维护培训通知");
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

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
