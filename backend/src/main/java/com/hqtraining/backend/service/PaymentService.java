package com.hqtraining.backend.service;

import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.PaymentPayRequest;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.PaymentRecordView;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    private static final RowMapper<PaymentRecordView> PAYMENT_ROW_MAPPER = (rs, rowNum) -> new PaymentRecordView(
            rs.getLong("id"),
            rs.getLong("enrollment_id"),
            rs.getString("enrollment_no"),
            rs.getLong("course_id"),
            rs.getString("course_no"),
            rs.getString("course_name"),
            rs.getLong("student_id"),
            rs.getString("student_no"),
            rs.getString("student_name"),
            rs.getString("company_name"),
            rs.getString("payment_type"),
            rs.getBigDecimal("receivable_amount"),
            rs.getBigDecimal("paid_amount"),
            rs.getString("payment_method"),
            rs.getString("payment_status"),
            toLocalDateTime(rs.getTimestamp("paid_at")),
            rs.getObject("handled_by", Long.class),
            rs.getString("handled_by_name"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public PaymentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<PaymentRecordView> getPayments(
            int pageNum,
            int pageSize,
            String keyword,
            String paymentStatus,
            Long courseId,
            CurrentUser currentUser
    ) {
        ensurePaymentReader(currentUser);
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        String likeKeyword = normalizeLikeKeyword(keyword);
        Long studentId = currentUser.hasRole("STUDENT") ? getStudentProfileIdByUserId(currentUser.id()) : null;

        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM payment_record pr
                INNER JOIN enrollment e ON pr.enrollment_id = e.id
                INNER JOIN course c ON pr.course_id = c.id
                INNER JOIN student_profile s ON pr.student_id = s.id
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                WHERE (? IS NULL OR pr.payment_status = ?)
                  AND (? IS NULL OR pr.course_id = ?)
                  AND (? IS NULL OR pr.student_id = ?)
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
                emptyToNull(paymentStatus), emptyToNull(paymentStatus),
                courseId, courseId,
                studentId, studentId,
                likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword
        );

        List<PaymentRecordView> list = jdbcTemplate.query(
                """
                SELECT
                    pr.id,
                    pr.enrollment_id,
                    e.enrollment_no,
                    pr.course_id,
                    c.course_no,
                    c.course_name,
                    pr.student_id,
                    s.student_no,
                    s.full_name AS student_name,
                    COALESCE(cc.company_name, '') AS company_name,
                    e.payment_type,
                    pr.receivable_amount,
                    pr.paid_amount,
                    pr.payment_method,
                    pr.payment_status,
                    pr.paid_at,
                    pr.handled_by,
                    COALESCE(u.display_name, '') AS handled_by_name,
                    pr.created_at,
                    pr.updated_at
                FROM payment_record pr
                INNER JOIN enrollment e ON pr.enrollment_id = e.id
                INNER JOIN course c ON pr.course_id = c.id
                INNER JOIN student_profile s ON pr.student_id = s.id
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                LEFT JOIN user_account u ON pr.handled_by = u.id
                WHERE (? IS NULL OR pr.payment_status = ?)
                  AND (? IS NULL OR pr.course_id = ?)
                  AND (? IS NULL OR pr.student_id = ?)
                  AND (
                    ? IS NULL
                    OR e.enrollment_no LIKE ?
                    OR c.course_no LIKE ?
                    OR c.course_name LIKE ?
                    OR s.student_no LIKE ?
                    OR s.full_name LIKE ?
                    OR COALESCE(cc.company_name, '') LIKE ?
                  )
                ORDER BY pr.created_at DESC, pr.id DESC
                LIMIT ? OFFSET ?
                """,
                PAYMENT_ROW_MAPPER,
                emptyToNull(paymentStatus), emptyToNull(paymentStatus),
                courseId, courseId,
                studentId, studentId,
                likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword,
                safePageSize, (safePageNum - 1) * safePageSize
        );

        return new PageResult<>(list, safePageNum, safePageSize, total == null ? 0 : total);
    }

    public PaymentRecordView getPaymentById(Long id, CurrentUser currentUser) {
        ensurePaymentReader(currentUser);
        List<PaymentRecordView> list = jdbcTemplate.query(
                """
                SELECT
                    pr.id,
                    pr.enrollment_id,
                    e.enrollment_no,
                    pr.course_id,
                    c.course_no,
                    c.course_name,
                    pr.student_id,
                    s.student_no,
                    s.full_name AS student_name,
                    COALESCE(cc.company_name, '') AS company_name,
                    e.payment_type,
                    pr.receivable_amount,
                    pr.paid_amount,
                    pr.payment_method,
                    pr.payment_status,
                    pr.paid_at,
                    pr.handled_by,
                    COALESCE(u.display_name, '') AS handled_by_name,
                    pr.created_at,
                    pr.updated_at
                FROM payment_record pr
                INNER JOIN enrollment e ON pr.enrollment_id = e.id
                INNER JOIN course c ON pr.course_id = c.id
                INNER JOIN student_profile s ON pr.student_id = s.id
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                LEFT JOIN user_account u ON pr.handled_by = u.id
                WHERE pr.id = ?
                """,
                PAYMENT_ROW_MAPPER,
                id
        );
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "收费记录不存在");
        }
        PaymentRecordView record = list.get(0);
        if (currentUser.hasRole("STUDENT")) {
            Long studentId = getStudentProfileIdByUserId(currentUser.id());
            if (!studentId.equals(record.studentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "学员仅可查看自己的缴费记录");
            }
        }
        return record;
    }

    public PaymentRecordView pay(Long id, PaymentPayRequest request, CurrentUser currentUser) {
        ensurePaymentOperator(currentUser);
        PaymentRecordView existing = getPaymentById(id, currentUser);
        ensureEnrollmentConfirmed(existing.enrollmentId());
        if ("PAID".equals(existing.paymentStatus()) || "CORPORATE_PAID".equals(existing.paymentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该收费记录已完成支付，不能重复收费");
        }

        String normalizedMethod = request.paymentMethod().trim().toUpperCase();
        if (!"CASH".equals(normalizedMethod) && !"TRANSFER".equals(normalizedMethod) && !"CORPORATE".equals(normalizedMethod)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "收费方式仅支持 CASH、TRANSFER、CORPORATE");
        }
        if (currentUser.hasRole("STUDENT")) {
            if ("CORPORATE".equals(existing.paymentType())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "企业付费课程由企业统一结算，学员无需自行缴费");
            }
            if ("CORPORATE".equals(normalizedMethod)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学员缴费不支持企业登记方式");
            }
        }

        BigDecimal paidAmount = request.paidAmount();
        String nextStatus;
        if ("CORPORATE".equals(existing.paymentType())) {
            nextStatus = "CORPORATE_PAID";
        } else {
            if (paidAmount.compareTo(existing.receivableAmount()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "个人付费的实收金额不能小于应收金额");
            }
            nextStatus = "PAID";
        }

        LocalDateTime now = LocalDateTime.now();
        int updatedRows = jdbcTemplate.update(
                """
                UPDATE payment_record
                SET paid_amount = ?, payment_method = ?, payment_status = ?, paid_at = ?, handled_by = ?, updated_at = ?
                WHERE id = ?
                """,
                paidAmount,
                normalizedMethod,
                nextStatus,
                Timestamp.valueOf(now),
                currentUser.id(),
                Timestamp.valueOf(now),
                id
        );
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "收费记录不存在");
        }

        writeOperationLog(currentUser.id(), id, "PAYMENT", "PAY", currentUser.hasRole("STUDENT") ? "学员提交缴费" : "现场收费");
        return getPaymentById(id, currentUser);
    }

    private void ensureEnrollmentConfirmed(Long enrollmentId) {
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM enrollment WHERE id = ?",
                String.class,
                enrollmentId
        );
        if (!"CONFIRMED".equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "仅已确认报名允许收费");
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

    private void ensurePaymentReader(CurrentUser currentUser) {
        if (currentUser.hasRole("SITE_STAFF") || currentUser.hasRole("STUDENT")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权访问收费信息");
    }

    private void ensurePaymentOperator(CurrentUser currentUser) {
        if (currentUser.hasRole("SITE_STAFF") || currentUser.hasRole("STUDENT")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权执行收费");
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
