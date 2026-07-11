package com.hqtraining.backend.service;

import com.hqtraining.backend.dto.InternalAccountCreateRequest;
import com.hqtraining.backend.dto.RegistrationReviewRequest;
import com.hqtraining.backend.dto.StudentRegisterRequest;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.ManagedAccountRecord;
import com.hqtraining.backend.model.RegistrationReviewRecord;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
public class AccountService {

    private static final Set<String> INTERNAL_ROLE_CODES = Set.of("ADMIN", "MANAGER", "EXECUTOR", "SITE_STAFF");
    private static final DateTimeFormatter STUDENT_NO_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private static final RowMapper<RegistrationReviewRecord> REGISTRATION_REVIEW_ROW_MAPPER = (rs, rowNum) -> new RegistrationReviewRecord(
            rs.getLong("user_id"),
            rs.getString("username"),
            rs.getString("display_name"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getString("company_name"),
            rs.getString("job_title"),
            rs.getString("education_level"),
            rs.getString("tech_level"),
            rs.getString("account_status"),
            rs.getString("reviewer_name"),
            rs.getString("review_comment"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            toLocalDateTime(rs.getTimestamp("reviewed_at"))
    );

    private static final RowMapper<ManagedAccountRecord> MANAGED_ACCOUNT_ROW_MAPPER = (rs, rowNum) -> new ManagedAccountRecord(
            rs.getLong("user_id"),
            rs.getString("username"),
            rs.getString("display_name"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getString("account_type"),
            rs.getString("role_code"),
            rs.getString("role_name"),
            rs.getString("account_status"),
            rs.getString("reviewer_name"),
            rs.getString("review_comment"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            toLocalDateTime(rs.getTimestamp("reviewed_at")),
            toLocalDateTime(rs.getTimestamp("last_login_at"))
    );

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public AccountService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public RegistrationReviewRecord registerStudent(StudentRegisterRequest request) {
        String username = normalizeRequired(request.username(), "用户名不能为空");
        String password = normalizeRequired(request.password(), "密码不能为空");
        String displayName = normalizeRequired(request.displayName(), "姓名不能为空");
        String companyName = normalizeRequired(request.companyName(), "所属企业不能为空");
        String phone = normalizeRequired(request.phone(), "手机号不能为空");
        String email = emptyToNull(request.email());

        ensureUniqueUsername(username);
        ensureUniquePhone(phone);
        ensureUniqueEmail(email);

        LocalDateTime now = LocalDateTime.now();
        Long userId = insertUserAccount(
                username,
                passwordEncoder.encode(password),
                displayName,
                email,
                phone,
                "STUDENT",
                "PENDING",
                null,
                null,
                null,
                now
        );
        bindRole(userId, "STUDENT", now);
        insertStudentProfile(userId, displayName, request, companyName, now);
        writeOperationLog(userId, "ACCOUNT", userId, "REGISTER", "提交学员注册申请");
        return getRegistrationByUserId(userId);
    }

    public List<RegistrationReviewRecord> getPendingRegistrations(CurrentUser currentUser) {
        ensureRegistrationReviewer(currentUser);
        return jdbcTemplate.query(
                """
                SELECT
                    ua.id AS user_id,
                    ua.username,
                    ua.display_name,
                    COALESCE(ua.phone, '') AS phone,
                    COALESCE(ua.email, '') AS email,
                    COALESCE(cc.company_name, '') AS company_name,
                    COALESCE(sp.job_title, '') AS job_title,
                    COALESCE(sp.education_level, '') AS education_level,
                    COALESCE(sp.tech_level, '') AS tech_level,
                    ua.account_status,
                    COALESCE(reviewer.display_name, '') AS reviewer_name,
                    COALESCE(ua.review_comment, '') AS review_comment,
                    ua.created_at,
                    ua.reviewed_at
                FROM user_account ua
                INNER JOIN user_role ur ON ua.id = ur.user_id
                INNER JOIN role r ON ur.role_id = r.id
                INNER JOIN student_profile sp ON sp.user_id = ua.id
                LEFT JOIN customer_company cc ON sp.company_id = cc.id
                LEFT JOIN user_account reviewer ON ua.reviewed_by = reviewer.id
                WHERE r.role_code = 'STUDENT'
                  AND ua.account_status = 'PENDING'
                ORDER BY ua.created_at ASC, ua.id ASC
                """,
                REGISTRATION_REVIEW_ROW_MAPPER
        );
    }

    public RegistrationReviewRecord reviewRegistration(Long userId, RegistrationReviewRequest request, CurrentUser currentUser) {
        ensureRegistrationReviewer(currentUser);
        RegistrationReviewRecord existing = getRegistrationByUserId(userId);
        if (!"PENDING".equals(existing.accountStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "仅待审核账号可执行审核");
        }

        boolean approved = Boolean.TRUE.equals(request.approved());
        String reviewComment = emptyToNull(request.reviewComment());
        if (!approved && reviewComment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "驳回时请填写审核说明");
        }

        String nextStatus = approved ? "ACTIVE" : "REJECTED";
        LocalDateTime now = LocalDateTime.now();
        int updatedRows = jdbcTemplate.update(
                """
                UPDATE user_account
                SET account_status = ?, reviewed_by = ?, reviewed_at = ?, review_comment = ?, updated_at = ?
                WHERE id = ?
                """,
                nextStatus,
                currentUser.id(),
                Timestamp.valueOf(now),
                reviewComment,
                Timestamp.valueOf(now),
                userId
        );
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "注册申请不存在");
        }

        writeOperationLog(
                currentUser.id(),
                "ACCOUNT",
                userId,
                approved ? "APPROVE" : "REJECT",
                approved ? "审核通过学员注册申请" : "驳回学员注册申请：" + reviewComment
        );
        return getRegistrationByUserId(userId);
    }

    public ManagedAccountRecord createInternalAccount(InternalAccountCreateRequest request, CurrentUser currentUser) {
        ensureAdmin(currentUser);

        String roleCode = normalizeRequired(request.roleCode(), "角色不能为空").toUpperCase();
        if (!INTERNAL_ROLE_CODES.contains(roleCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持创建内部管理账号");
        }

        String username = normalizeRequired(request.username(), "用户名不能为空");
        String password = normalizeRequired(request.password(), "密码不能为空");
        String displayName = normalizeRequired(request.displayName(), "姓名不能为空");
        String phone = emptyToNull(request.phone());
        String email = emptyToNull(request.email());

        ensureUniqueUsername(username);
        ensureUniquePhone(phone);
        ensureUniqueEmail(email);

        LocalDateTime now = LocalDateTime.now();
        Long userId = insertUserAccount(
                username,
                passwordEncoder.encode(password),
                displayName,
                email,
                phone,
                roleCode,
                "ACTIVE",
                currentUser.id(),
                "管理员创建内部账号",
                now,
                now
        );
        bindRole(userId, roleCode, now);
        writeOperationLog(currentUser.id(), "ACCOUNT", userId, "CREATE", "创建内部账号：" + displayName);
        return getManagedAccountByUserId(userId);
    }

    public List<ManagedAccountRecord> getManagedAccounts(CurrentUser currentUser) {
        ensureAdmin(currentUser);
        return jdbcTemplate.query(
                """
                SELECT
                    ua.id AS user_id,
                    ua.username,
                    ua.display_name,
                    COALESCE(ua.phone, '') AS phone,
                    COALESCE(ua.email, '') AS email,
                    ua.account_type,
                    r.role_code,
                    r.role_name,
                    ua.account_status,
                    COALESCE(reviewer.display_name, '') AS reviewer_name,
                    COALESCE(ua.review_comment, '') AS review_comment,
                    ua.created_at,
                    ua.reviewed_at,
                    ua.last_login_at
                FROM user_account ua
                INNER JOIN user_role ur ON ua.id = ur.user_id
                INNER JOIN role r ON ur.role_id = r.id
                LEFT JOIN user_account reviewer ON ua.reviewed_by = reviewer.id
                ORDER BY ua.created_at DESC, ua.id DESC
                """,
                MANAGED_ACCOUNT_ROW_MAPPER
        );
    }

    private RegistrationReviewRecord getRegistrationByUserId(Long userId) {
        List<RegistrationReviewRecord> records = jdbcTemplate.query(
                """
                SELECT
                    ua.id AS user_id,
                    ua.username,
                    ua.display_name,
                    COALESCE(ua.phone, '') AS phone,
                    COALESCE(ua.email, '') AS email,
                    COALESCE(cc.company_name, '') AS company_name,
                    COALESCE(sp.job_title, '') AS job_title,
                    COALESCE(sp.education_level, '') AS education_level,
                    COALESCE(sp.tech_level, '') AS tech_level,
                    ua.account_status,
                    COALESCE(reviewer.display_name, '') AS reviewer_name,
                    COALESCE(ua.review_comment, '') AS review_comment,
                    ua.created_at,
                    ua.reviewed_at
                FROM user_account ua
                INNER JOIN user_role ur ON ua.id = ur.user_id
                INNER JOIN role r ON ur.role_id = r.id
                INNER JOIN student_profile sp ON sp.user_id = ua.id
                LEFT JOIN customer_company cc ON sp.company_id = cc.id
                LEFT JOIN user_account reviewer ON ua.reviewed_by = reviewer.id
                WHERE ua.id = ?
                  AND r.role_code = 'STUDENT'
                """,
                REGISTRATION_REVIEW_ROW_MAPPER,
                userId
        );
        if (records.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "注册申请不存在");
        }
        return records.get(0);
    }

    private ManagedAccountRecord getManagedAccountByUserId(Long userId) {
        List<ManagedAccountRecord> records = jdbcTemplate.query(
                """
                SELECT
                    ua.id AS user_id,
                    ua.username,
                    ua.display_name,
                    COALESCE(ua.phone, '') AS phone,
                    COALESCE(ua.email, '') AS email,
                    ua.account_type,
                    r.role_code,
                    r.role_name,
                    ua.account_status,
                    COALESCE(reviewer.display_name, '') AS reviewer_name,
                    COALESCE(ua.review_comment, '') AS review_comment,
                    ua.created_at,
                    ua.reviewed_at,
                    ua.last_login_at
                FROM user_account ua
                INNER JOIN user_role ur ON ua.id = ur.user_id
                INNER JOIN role r ON ur.role_id = r.id
                LEFT JOIN user_account reviewer ON ua.reviewed_by = reviewer.id
                WHERE ua.id = ?
                """,
                MANAGED_ACCOUNT_ROW_MAPPER,
                userId
        );
        if (records.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "账号不存在");
        }
        return records.get(0);
    }

    private Long insertUserAccount(
            String username,
            String passwordHash,
            String displayName,
            String email,
            String phone,
            String accountType,
            String accountStatus,
            Long reviewedBy,
            String reviewComment,
            LocalDateTime reviewedAt,
            LocalDateTime now
    ) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO user_account (
                        username, password_hash, display_name, email, phone, account_type, account_status,
                        review_comment, reviewed_by, reviewed_at, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, displayName);
            statement.setString(4, email);
            statement.setString(5, phone);
            statement.setString(6, accountType);
            statement.setString(7, accountStatus);
            statement.setString(8, reviewComment);
            statement.setObject(9, reviewedBy);
            statement.setTimestamp(10, reviewedAt == null ? null : Timestamp.valueOf(reviewedAt));
            statement.setTimestamp(11, Timestamp.valueOf(now));
            statement.setTimestamp(12, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "账号创建失败");
        }
        return key.longValue();
    }

    private void bindRole(Long userId, String roleCode, LocalDateTime now) {
        Long roleId = getRoleId(roleCode);
        jdbcTemplate.update(
                """
                INSERT INTO user_role (user_id, role_id, created_at)
                VALUES (?, ?, ?)
                """,
                userId,
                roleId,
                Timestamp.valueOf(now)
        );
    }

    private void insertStudentProfile(
            Long userId,
            String displayName,
            StudentRegisterRequest request,
            String companyName,
            LocalDateTime now
    ) {
        Long companyId = resolveCompanyId(companyName);
        jdbcTemplate.update(
                """
                INSERT INTO student_profile (
                    user_id, student_no, full_name, gender, company_id, job_title, education_level, tech_level,
                    phone, email, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                generateStudentNo(),
                displayName,
                emptyToNull(request.gender()),
                companyId,
                emptyToNull(request.jobTitle()),
                emptyToNull(request.educationLevel()),
                emptyToNull(request.techLevel()),
                normalizeRequired(request.phone(), "手机号不能为空"),
                emptyToNull(request.email()),
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );
    }

    private Long resolveCompanyId(String companyName) {
        List<Long> existingIds = jdbcTemplate.queryForList(
                "SELECT id FROM customer_company WHERE company_name = ?",
                Long.class,
                companyName
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
                    ) VALUES (?, '企业客户', '注册学员自动建立企业资料', ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, companyName);
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

    private Long getRoleId(String roleCode) {
        List<Long> roleIds = jdbcTemplate.queryForList(
                "SELECT id FROM role WHERE role_code = ?",
                Long.class,
                roleCode
        );
        if (roleIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色不存在");
        }
        return roleIds.get(0);
    }

    private void ensureUniqueUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_account WHERE username = ?",
                Integer.class,
                username
        );
        if (count != null && count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在，请更换后重试");
        }
    }

    private void ensureUniquePhone(String phone) {
        if (phone == null) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_account WHERE phone = ?",
                Integer.class,
                phone
        );
        if (count != null && count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "手机号已绑定其他账号");
        }
    }

    private void ensureUniqueEmail(String email) {
        if (email == null) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM user_account WHERE email = ?",
                Integer.class,
                email
        );
        if (count != null && count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已绑定其他账号");
        }
    }

    private String generateStudentNo() {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM student_profile", Long.class);
        long nextId = (maxId == null ? 0 : maxId) + 1;
        return "STU" + LocalDate.now().format(STUDENT_NO_DATE_FORMATTER) + "%03d".formatted(nextId);
    }

    private void ensureRegistrationReviewer(CurrentUser currentUser) {
        if (currentUser.hasRole("ADMIN") || currentUser.hasRole("EXECUTOR")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权审核注册申请");
    }

    private void ensureAdmin(CurrentUser currentUser) {
        if (currentUser.hasRole("ADMIN")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅系统管理员可维护内部账号");
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

    private String normalizeRequired(String value, String message) {
        String normalized = emptyToNull(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return normalized;
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
