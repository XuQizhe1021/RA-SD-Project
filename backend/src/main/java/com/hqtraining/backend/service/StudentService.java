package com.hqtraining.backend.service;

import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.StudentSaveRequest;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.StudentProfileRecord;
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
public class StudentService {

    private static final RowMapper<StudentProfileRecord> STUDENT_ROW_MAPPER = (rs, rowNum) -> new StudentProfileRecord(
            rs.getLong("id"),
            rs.getObject("user_id", Long.class),
            rs.getString("student_no"),
            rs.getString("full_name"),
            rs.getString("gender"),
            rs.getObject("company_id", Long.class),
            rs.getString("company_name"),
            rs.getString("job_title"),
            rs.getString("education_level"),
            rs.getString("tech_level"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public StudentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<StudentProfileRecord> getStudents(
            int pageNum,
            int pageSize,
            String keyword,
            Long companyId,
            CurrentUser currentUser
    ) {
        ensureExecutor(currentUser);
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        String likeKeyword = normalizeLikeKeyword(keyword);

        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM student_profile s
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                WHERE (? IS NULL OR s.company_id = ?)
                  AND (
                    ? IS NULL
                    OR s.student_no LIKE ?
                    OR s.full_name LIKE ?
                    OR COALESCE(cc.company_name, '') LIKE ?
                    OR COALESCE(s.phone, '') LIKE ?
                    OR COALESCE(s.email, '') LIKE ?
                  )
                """,
                Long.class,
                companyId, companyId,
                likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword
        );

        List<StudentProfileRecord> list = jdbcTemplate.query(
                """
                SELECT
                    s.id,
                    s.user_id,
                    s.student_no,
                    s.full_name,
                    COALESCE(s.gender, '') AS gender,
                    s.company_id,
                    COALESCE(cc.company_name, '') AS company_name,
                    COALESCE(s.job_title, '') AS job_title,
                    COALESCE(s.education_level, '') AS education_level,
                    COALESCE(s.tech_level, '') AS tech_level,
                    COALESCE(s.phone, '') AS phone,
                    COALESCE(s.email, '') AS email,
                    s.created_at,
                    s.updated_at
                FROM student_profile s
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                WHERE (? IS NULL OR s.company_id = ?)
                  AND (
                    ? IS NULL
                    OR s.student_no LIKE ?
                    OR s.full_name LIKE ?
                    OR COALESCE(cc.company_name, '') LIKE ?
                    OR COALESCE(s.phone, '') LIKE ?
                    OR COALESCE(s.email, '') LIKE ?
                  )
                ORDER BY s.created_at DESC, s.id DESC
                LIMIT ? OFFSET ?
                """,
                STUDENT_ROW_MAPPER,
                companyId, companyId,
                likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword,
                safePageSize, (safePageNum - 1) * safePageSize
        );

        return new PageResult<>(list, safePageNum, safePageSize, total == null ? 0 : total);
    }

    public StudentProfileRecord getStudentById(Long id, CurrentUser currentUser) {
        ensureExecutor(currentUser);
        List<StudentProfileRecord> list = jdbcTemplate.query(
                """
                SELECT
                    s.id,
                    s.user_id,
                    s.student_no,
                    s.full_name,
                    COALESCE(s.gender, '') AS gender,
                    s.company_id,
                    COALESCE(cc.company_name, '') AS company_name,
                    COALESCE(s.job_title, '') AS job_title,
                    COALESCE(s.education_level, '') AS education_level,
                    COALESCE(s.tech_level, '') AS tech_level,
                    COALESCE(s.phone, '') AS phone,
                    COALESCE(s.email, '') AS email,
                    s.created_at,
                    s.updated_at
                FROM student_profile s
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                WHERE s.id = ?
                """,
                STUDENT_ROW_MAPPER,
                id
        );
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学员档案不存在");
        }
        return list.get(0);
    }

    public StudentProfileRecord createStudent(StudentSaveRequest request, CurrentUser currentUser) {
        ensureExecutor(currentUser);
        Long companyId = resolveCompanyId(request.companyName());
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO student_profile (
                        user_id, student_no, full_name, gender, company_id, job_title, education_level, tech_level,
                        phone, email, created_at, updated_at
                    ) VALUES (NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, generateStudentNo());
            statement.setString(2, request.fullName().trim());
            statement.setString(3, emptyToNull(request.gender()));
            statement.setLong(4, companyId);
            statement.setString(5, emptyToNull(request.jobTitle()));
            statement.setString(6, emptyToNull(request.educationLevel()));
            statement.setString(7, emptyToNull(request.techLevel()));
            statement.setString(8, emptyToNull(request.phone()));
            statement.setString(9, emptyToNull(request.email()));
            statement.setTimestamp(10, Timestamp.valueOf(now));
            statement.setTimestamp(11, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "学员档案创建失败");
        }
        writeOperationLog(currentUser.id(), "STUDENT", key.longValue(), "CREATE", "新增学员档案");
        return getStudentById(key.longValue(), currentUser);
    }

    public StudentProfileRecord updateStudent(Long id, StudentSaveRequest request, CurrentUser currentUser) {
        ensureExecutor(currentUser);
        getStudentById(id, currentUser);
        Long companyId = resolveCompanyId(request.companyName());

        int updatedRows = jdbcTemplate.update(
                """
                UPDATE student_profile
                SET full_name = ?, gender = ?, company_id = ?, job_title = ?, education_level = ?,
                    tech_level = ?, phone = ?, email = ?, updated_at = ?
                WHERE id = ?
                """,
                request.fullName().trim(),
                emptyToNull(request.gender()),
                companyId,
                emptyToNull(request.jobTitle()),
                emptyToNull(request.educationLevel()),
                emptyToNull(request.techLevel()),
                emptyToNull(request.phone()),
                emptyToNull(request.email()),
                Timestamp.valueOf(LocalDateTime.now()),
                id
        );
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学员档案不存在");
        }
        return getStudentById(id, currentUser);
    }

    private Long resolveCompanyId(String companyName) {
        String normalizedName = emptyToNull(companyName);
        if (normalizedName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所属企业不能为空");
        }
        List<Long> existingIds = jdbcTemplate.queryForList(
                "SELECT id FROM customer_company WHERE company_name = ?",
                Long.class,
                normalizedName
        );
        if (!existingIds.isEmpty()) {
            return existingIds.get(0);
        }

        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO customer_company (
                        company_name, company_type, remark, created_at, updated_at
                    ) VALUES (?, '企业客户', '学员档案自动补录', ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, normalizedName);
            statement.setTimestamp(2, Timestamp.valueOf(now));
            statement.setTimestamp(3, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "企业信息创建失败");
        }
        return key.longValue();
    }

    private void ensureExecutor(CurrentUser currentUser) {
        if (currentUser.hasRole("EXECUTOR")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅执行人可维护学员档案");
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

    private String generateStudentNo() {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM student_profile", Long.class);
        long nextId = (maxId == null ? 0 : maxId) + 1;
        return "STU20260711%03d".formatted(nextId);
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
