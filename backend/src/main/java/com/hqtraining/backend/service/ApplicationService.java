package com.hqtraining.backend.service;

import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.ApplicationApproveRequest;
import com.hqtraining.backend.dto.ApplicationSaveRequest;
import com.hqtraining.backend.model.ApplicationOptionRecord;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.TrainingApplicationRecord;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationService {

    private static final RowMapper<TrainingApplicationRecord> APPLICATION_ROW_MAPPER = (rs, rowNum) -> new TrainingApplicationRecord(
            rs.getLong("id"),
            rs.getString("application_no"),
            rs.getLong("company_id"),
            rs.getString("company_name"),
            rs.getObject("applicant_user_id", Long.class),
            rs.getString("applicant_name"),
            rs.getString("topic"),
            rs.getObject("expected_start_date", java.time.LocalDate.class),
            rs.getObject("expected_end_date", java.time.LocalDate.class),
            rs.getInt("attendee_count"),
            rs.getBigDecimal("budget_amount"),
            rs.getString("requirement_desc"),
            rs.getString("status"),
            rs.getString("approval_comment"),
            rs.getObject("approved_by", Long.class),
            rs.getString("approved_by_name"),
            toLocalDateTime(rs.getTimestamp("approved_at")),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private static final RowMapper<ApplicationOptionRecord> APPLICATION_OPTION_ROW_MAPPER = (rs, rowNum) -> new ApplicationOptionRecord(
            rs.getLong("id"),
            rs.getString("application_no"),
            rs.getString("company_name"),
            rs.getString("topic"),
            rs.getObject("expected_start_date", java.time.LocalDate.class),
            rs.getObject("expected_end_date", java.time.LocalDate.class),
            rs.getInt("attendee_count"),
            rs.getBigDecimal("budget_amount")
    );

    private final JdbcTemplate jdbcTemplate;

    public ApplicationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<TrainingApplicationRecord> getApplications(
            int pageNum,
            int pageSize,
            String keyword,
            String status,
            CurrentUser currentUser
    ) {
        ensureReadable(currentUser);
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        String likeKeyword = normalizeLikeKeyword(keyword);

        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM training_application ta
                INNER JOIN customer_company cc ON ta.company_id = cc.id
                LEFT JOIN user_account applicant ON ta.applicant_user_id = applicant.id
                WHERE (? IS NULL OR ta.status = ?)
                  AND (
                    ? IS NULL
                    OR ta.application_no LIKE ?
                    OR cc.company_name LIKE ?
                    OR ta.topic LIKE ?
                    OR COALESCE(applicant.display_name, '') LIKE ?
                  )
                """,
                Long.class,
                emptyToNull(status), emptyToNull(status),
                likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword
        );

        List<TrainingApplicationRecord> list = jdbcTemplate.query(
                """
                SELECT
                    ta.id,
                    ta.application_no,
                    ta.company_id,
                    cc.company_name,
                    ta.applicant_user_id,
                    COALESCE(applicant.display_name, '') AS applicant_name,
                    ta.topic,
                    ta.expected_start_date,
                    ta.expected_end_date,
                    ta.attendee_count,
                    ta.budget_amount,
                    ta.requirement_desc,
                    ta.status,
                    ta.approval_comment,
                    ta.approved_by,
                    COALESCE(approver.display_name, '') AS approved_by_name,
                    ta.approved_at,
                    ta.created_at,
                    ta.updated_at
                FROM training_application ta
                INNER JOIN customer_company cc ON ta.company_id = cc.id
                LEFT JOIN user_account applicant ON ta.applicant_user_id = applicant.id
                LEFT JOIN user_account approver ON ta.approved_by = approver.id
                WHERE (? IS NULL OR ta.status = ?)
                  AND (
                    ? IS NULL
                    OR ta.application_no LIKE ?
                    OR cc.company_name LIKE ?
                    OR ta.topic LIKE ?
                    OR COALESCE(applicant.display_name, '') LIKE ?
                  )
                ORDER BY ta.created_at DESC, ta.id DESC
                LIMIT ? OFFSET ?
                """,
                APPLICATION_ROW_MAPPER,
                emptyToNull(status), emptyToNull(status),
                likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword,
                safePageSize, (safePageNum - 1) * safePageSize
        );

        return new PageResult<>(list, safePageNum, safePageSize, total == null ? 0 : total);
    }

    public TrainingApplicationRecord getApplicationById(Long id, CurrentUser currentUser) {
        ensureReadable(currentUser);
        List<TrainingApplicationRecord> list = jdbcTemplate.query(
                """
                SELECT
                    ta.id,
                    ta.application_no,
                    ta.company_id,
                    cc.company_name,
                    ta.applicant_user_id,
                    COALESCE(applicant.display_name, '') AS applicant_name,
                    ta.topic,
                    ta.expected_start_date,
                    ta.expected_end_date,
                    ta.attendee_count,
                    ta.budget_amount,
                    ta.requirement_desc,
                    ta.status,
                    ta.approval_comment,
                    ta.approved_by,
                    COALESCE(approver.display_name, '') AS approved_by_name,
                    ta.approved_at,
                    ta.created_at,
                    ta.updated_at
                FROM training_application ta
                INNER JOIN customer_company cc ON ta.company_id = cc.id
                LEFT JOIN user_account applicant ON ta.applicant_user_id = applicant.id
                LEFT JOIN user_account approver ON ta.approved_by = approver.id
                WHERE ta.id = ?
                """,
                APPLICATION_ROW_MAPPER,
                id
        );
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "培训申请不存在");
        }
        return list.get(0);
    }

    public TrainingApplicationRecord createApplication(ApplicationSaveRequest request, CurrentUser currentUser) {
        ensureManager(currentUser);
        validateDateRange(request.expectedStartDate(), request.expectedEndDate());
        Long companyId = resolveCompanyId(request.companyName());
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO training_application (
                        application_no, company_id, applicant_user_id, topic, expected_start_date, expected_end_date,
                        attendee_count, budget_amount, requirement_desc, status, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, generateApplicationNo());
            statement.setLong(2, companyId);
            statement.setLong(3, currentUser.id());
            statement.setString(4, request.topic().trim());
            statement.setDate(5, request.expectedStartDate() == null ? null : Date.valueOf(request.expectedStartDate()));
            statement.setDate(6, request.expectedEndDate() == null ? null : Date.valueOf(request.expectedEndDate()));
            statement.setInt(7, request.attendeeCount());
            statement.setBigDecimal(8, request.budgetAmount());
            statement.setString(9, emptyToNull(request.requirementDesc()));
            statement.setTimestamp(10, Timestamp.valueOf(now));
            statement.setTimestamp(11, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "培训申请创建失败");
        }
        writeOperationLog(currentUser.id(), "APPLICATION", key.longValue(), "CREATE", "新增培训申请");
        return getApplicationById(key.longValue(), currentUser);
    }

    public TrainingApplicationRecord approveApplication(Long id, ApplicationApproveRequest request, CurrentUser currentUser) {
        ensureManager(currentUser);
        TrainingApplicationRecord existing = getApplicationById(id, currentUser);
        if (!"PENDING".equals(existing.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只有待审批的申请可以执行审批");
        }

        boolean approved = Boolean.TRUE.equals(request.approved());
        String nextStatus = approved ? "APPROVED" : "REJECTED";
        String approvalComment = emptyToNull(request.approvalComment());
        if (!approved && approvalComment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "驳回申请时必须填写审批意见");
        }

        LocalDateTime now = LocalDateTime.now();
        int updatedRows = jdbcTemplate.update(
                """
                UPDATE training_application
                SET status = ?, approval_comment = ?, approved_by = ?, approved_at = ?, updated_at = ?
                WHERE id = ?
                """,
                nextStatus,
                approvalComment,
                currentUser.id(),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                id
        );
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "培训申请不存在");
        }

        writeOperationLog(
                currentUser.id(),
                "APPLICATION",
                id,
                approved ? "APPROVE" : "REJECT",
                approved ? "培训申请审批通过" : "培训申请审批驳回：" + approvalComment
        );
        return getApplicationById(id, currentUser);
    }

    public List<ApplicationOptionRecord> getApprovedApplicationOptions(CurrentUser currentUser) {
        if (!(currentUser.hasRole("MANAGER") || currentUser.hasRole("EXECUTOR"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权查看培训申请选项");
        }
        return jdbcTemplate.query(
                """
                SELECT
                    ta.id,
                    ta.application_no,
                    cc.company_name,
                    ta.topic,
                    ta.expected_start_date,
                    ta.expected_end_date,
                    ta.attendee_count,
                    ta.budget_amount
                FROM training_application ta
                INNER JOIN customer_company cc ON ta.company_id = cc.id
                WHERE ta.status = 'APPROVED'
                ORDER BY ta.created_at DESC, ta.id DESC
                """,
                APPLICATION_OPTION_ROW_MAPPER
        );
    }

    public void markApplicationCourseCreated(Long applicationId) {
        if (applicationId == null) {
            return;
        }
        jdbcTemplate.update(
                """
                UPDATE training_application
                SET status = 'COURSE_CREATED', updated_at = ?
                WHERE id = ?
                  AND status = 'APPROVED'
                """,
                Timestamp.valueOf(LocalDateTime.now()),
                applicationId
        );
    }

    public void revertApplicationToApproved(Long applicationId) {
        if (applicationId == null) {
            return;
        }
        Integer courseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM course WHERE application_id = ?",
                Integer.class,
                applicationId
        );
        if (courseCount != null && courseCount > 0) {
            return;
        }
        jdbcTemplate.update(
                """
                UPDATE training_application
                SET status = 'APPROVED', updated_at = ?
                WHERE id = ?
                  AND status = 'COURSE_CREATED'
                """,
                Timestamp.valueOf(LocalDateTime.now()),
                applicationId
        );
    }

    private Long resolveCompanyId(String companyName) {
        String normalizedName = emptyToNull(companyName);
        if (normalizedName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "企业名称不能为空");
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
                    ) VALUES (?, '企业客户', '培训申请自动补录', ?, ?)
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

    private void validateDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "预计结束日期不能早于预计开始日期");
        }
    }

    private void ensureManager(CurrentUser currentUser) {
        if (currentUser.hasRole("MANAGER")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅经理可维护培训申请");
    }

    private void ensureReadable(CurrentUser currentUser) {
        if (currentUser.hasRole("MANAGER") || currentUser.hasRole("EXECUTOR")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权查看培训申请");
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

    private String generateApplicationNo() {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM training_application", Long.class);
        long nextId = (maxId == null ? 0 : maxId) + 1;
        return "APP20260711%03d".formatted(nextId);
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
