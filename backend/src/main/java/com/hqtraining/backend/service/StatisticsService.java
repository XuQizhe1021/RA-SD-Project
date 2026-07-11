package com.hqtraining.backend.service;

import com.hqtraining.backend.model.CourseStatisticsRecord;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.LecturerStatisticsRecord;
import com.hqtraining.backend.model.RevenueDetailRecord;
import com.hqtraining.backend.model.RevenueStatisticsResponse;
import com.hqtraining.backend.model.StudentStatisticsRecord;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StatisticsService {

    private static final RowMapper<CourseStatisticsRecord> COURSE_STATISTICS_ROW_MAPPER = (rs, rowNum) -> new CourseStatisticsRecord(
            rs.getLong("course_id"),
            rs.getString("course_no"),
            rs.getString("course_name"),
            rs.getString("lecturer_name"),
            rs.getTimestamp("start_time").toLocalDateTime(),
            rs.getTimestamp("end_time").toLocalDateTime(),
            rs.getInt("quota"),
            rs.getInt("enrollment_count"),
            rs.getInt("attendance_count"),
            rs.getBigDecimal("paid_amount_total"),
            rs.getBigDecimal("average_rating")
    );

    private static final RowMapper<StudentStatisticsRecord> STUDENT_STATISTICS_ROW_MAPPER = (rs, rowNum) -> new StudentStatisticsRecord(
            rs.getLong("student_id"),
            rs.getString("student_no"),
            rs.getString("student_name"),
            rs.getString("company_name"),
            rs.getInt("attendance_count"),
            rs.getInt("enrollment_count"),
            rs.getBigDecimal("paid_amount_total"),
            rs.getBigDecimal("average_rating")
    );

    private static final RowMapper<LecturerStatisticsRecord> LECTURER_STATISTICS_ROW_MAPPER = (rs, rowNum) -> new LecturerStatisticsRecord(
            rs.getLong("lecturer_id"),
            rs.getString("lecturer_no"),
            rs.getString("lecturer_name"),
            rs.getInt("course_count"),
            rs.getInt("attendance_count"),
            rs.getBigDecimal("average_rating"),
            rs.getBigDecimal("fee_amount_total")
    );

    private static final RowMapper<RevenueDetailRecord> REVENUE_DETAIL_ROW_MAPPER = (rs, rowNum) -> new RevenueDetailRecord(
            rs.getLong("payment_id"),
            rs.getString("course_name"),
            rs.getString("student_name"),
            rs.getBigDecimal("receivable_amount"),
            rs.getBigDecimal("paid_amount"),
            rs.getString("payment_method"),
            toLocalDateTime(rs.getTimestamp("paid_at")),
            rs.getString("handled_by_name")
    );

    private final JdbcTemplate jdbcTemplate;

    public StatisticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CourseStatisticsRecord> getCourseStatistics(
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            CurrentUser currentUser
    ) {
        ensureStatisticsReader(currentUser);
        FilterRange range = normalizeRange(startDate, endDate);
        String likeKeyword = normalizeLikeKeyword(keyword);

        // 通过分维度聚合子查询避免报名、签到、收费、评价相互连接后产生笛卡尔放大。
        return jdbcTemplate.query(
                """
                SELECT
                    c.id AS course_id,
                    c.course_no,
                    c.course_name,
                    COALESCE(lp.full_name, '未分配讲师') AS lecturer_name,
                    c.start_time,
                    c.end_time,
                    c.quota,
                    COALESCE(enr.enrollment_count, 0) AS enrollment_count,
                    COALESCE(att.attendance_count, 0) AS attendance_count,
                    COALESCE(pay.paid_amount_total, 0.00) AS paid_amount_total,
                    eval.average_rating
                FROM course c
                LEFT JOIN lecturer_profile lp ON c.lecturer_id = lp.id
                LEFT JOIN (
                    SELECT e.course_id, COUNT(*) AS enrollment_count
                    FROM enrollment e
                    GROUP BY e.course_id
                ) enr ON enr.course_id = c.id
                LEFT JOIN (
                    SELECT ar.course_id, COUNT(*) AS attendance_count
                    FROM attendance_record ar
                    WHERE ar.status = 'CHECKED_IN'
                    GROUP BY ar.course_id
                ) att ON att.course_id = c.id
                LEFT JOIN (
                    SELECT pr.course_id, SUM(pr.paid_amount) AS paid_amount_total
                    FROM payment_record pr
                    GROUP BY pr.course_id
                ) pay ON pay.course_id = c.id
                LEFT JOIN (
                    SELECT ce.course_id, ROUND(AVG(ce.rating), 1) AS average_rating
                    FROM course_evaluation ce
                    GROUP BY ce.course_id
                ) eval ON eval.course_id = c.id
                WHERE c.start_time >= ?
                  AND c.start_time < ?
                  AND (
                    ? IS NULL
                    OR c.course_no LIKE ?
                    OR c.course_name LIKE ?
                    OR COALESCE(lp.full_name, '') LIKE ?
                  )
                ORDER BY c.start_time DESC, c.id DESC
                """,
                COURSE_STATISTICS_ROW_MAPPER,
                range.startAt(), range.endExclusive(),
                likeKeyword, likeKeyword, likeKeyword, likeKeyword
        );
    }

    public List<StudentStatisticsRecord> getStudentStatistics(
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            CurrentUser currentUser
    ) {
        ensureStatisticsReader(currentUser);
        FilterRange range = normalizeRange(startDate, endDate);
        String likeKeyword = normalizeLikeKeyword(keyword);

        return jdbcTemplate.query(
                """
                SELECT
                    s.id AS student_id,
                    s.student_no,
                    s.full_name AS student_name,
                    COALESCE(cc.company_name, '') AS company_name,
                    COALESCE(att.attendance_count, 0) AS attendance_count,
                    COALESCE(enr.enrollment_count, 0) AS enrollment_count,
                    COALESCE(pay.paid_amount_total, 0.00) AS paid_amount_total,
                    eval.average_rating
                FROM student_profile s
                LEFT JOIN customer_company cc ON s.company_id = cc.id
                LEFT JOIN (
                    SELECT e.student_id, COUNT(*) AS enrollment_count
                    FROM enrollment e
                    INNER JOIN course c ON e.course_id = c.id
                    WHERE c.start_time >= ? AND c.start_time < ?
                    GROUP BY e.student_id
                ) enr ON enr.student_id = s.id
                LEFT JOIN (
                    SELECT ar.student_id, COUNT(*) AS attendance_count
                    FROM attendance_record ar
                    INNER JOIN course c ON ar.course_id = c.id
                    WHERE ar.status = 'CHECKED_IN'
                      AND c.start_time >= ?
                      AND c.start_time < ?
                    GROUP BY ar.student_id
                ) att ON att.student_id = s.id
                LEFT JOIN (
                    SELECT pr.student_id, SUM(pr.paid_amount) AS paid_amount_total
                    FROM payment_record pr
                    INNER JOIN course c ON pr.course_id = c.id
                    WHERE c.start_time >= ? AND c.start_time < ?
                    GROUP BY pr.student_id
                ) pay ON pay.student_id = s.id
                LEFT JOIN (
                    SELECT ce.student_id, ROUND(AVG(ce.rating), 1) AS average_rating
                    FROM course_evaluation ce
                    INNER JOIN course c ON ce.course_id = c.id
                    WHERE c.start_time >= ? AND c.start_time < ?
                    GROUP BY ce.student_id
                ) eval ON eval.student_id = s.id
                WHERE (
                    COALESCE(enr.enrollment_count, 0) > 0
                    OR COALESCE(att.attendance_count, 0) > 0
                    OR COALESCE(pay.paid_amount_total, 0.00) > 0
                    OR eval.average_rating IS NOT NULL
                  )
                  AND (
                    ? IS NULL
                    OR s.student_no LIKE ?
                    OR s.full_name LIKE ?
                    OR COALESCE(cc.company_name, '') LIKE ?
                  )
                ORDER BY COALESCE(att.attendance_count, 0) DESC, s.id ASC
                """,
                STUDENT_STATISTICS_ROW_MAPPER,
                range.startAt(), range.endExclusive(),
                range.startAt(), range.endExclusive(),
                range.startAt(), range.endExclusive(),
                range.startAt(), range.endExclusive(),
                likeKeyword, likeKeyword, likeKeyword, likeKeyword
        );
    }

    public List<LecturerStatisticsRecord> getLecturerStatistics(
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            CurrentUser currentUser
    ) {
        ensureStatisticsReader(currentUser);
        FilterRange range = normalizeRange(startDate, endDate);
        String likeKeyword = normalizeLikeKeyword(keyword);

        return jdbcTemplate.query(
                """
                SELECT
                    lp.id AS lecturer_id,
                    lp.lecturer_no,
                    lp.full_name AS lecturer_name,
                    COALESCE(course_stats.course_count, 0) AS course_count,
                    COALESCE(att.attendance_count, 0) AS attendance_count,
                    eval.average_rating,
                    COALESCE(course_stats.course_count, 0) * COALESCE(lp.fee_standard, 0.00) AS fee_amount_total
                FROM lecturer_profile lp
                LEFT JOIN (
                    SELECT c.lecturer_id, COUNT(*) AS course_count
                    FROM course c
                    WHERE c.start_time >= ? AND c.start_time < ?
                      AND c.status IN ('PUBLISHED', 'ONGOING', 'FINISHED')
                    GROUP BY c.lecturer_id
                ) course_stats ON course_stats.lecturer_id = lp.id
                LEFT JOIN (
                    SELECT c.lecturer_id, COUNT(*) AS attendance_count
                    FROM attendance_record ar
                    INNER JOIN course c ON ar.course_id = c.id
                    WHERE ar.status = 'CHECKED_IN'
                      AND c.start_time >= ?
                      AND c.start_time < ?
                    GROUP BY c.lecturer_id
                ) att ON att.lecturer_id = lp.id
                LEFT JOIN (
                    SELECT c.lecturer_id, ROUND(AVG(ce.rating), 1) AS average_rating
                    FROM course_evaluation ce
                    INNER JOIN course c ON ce.course_id = c.id
                    WHERE c.start_time >= ? AND c.start_time < ?
                    GROUP BY c.lecturer_id
                ) eval ON eval.lecturer_id = lp.id
                WHERE COALESCE(course_stats.course_count, 0) > 0
                  AND (
                    ? IS NULL
                    OR lp.lecturer_no LIKE ?
                    OR lp.full_name LIKE ?
                    OR COALESCE(lp.specialty, '') LIKE ?
                  )
                ORDER BY COALESCE(course_stats.course_count, 0) DESC, lp.id ASC
                """,
                LECTURER_STATISTICS_ROW_MAPPER,
                range.startAt(), range.endExclusive(),
                range.startAt(), range.endExclusive(),
                range.startAt(), range.endExclusive(),
                likeKeyword, likeKeyword, likeKeyword, likeKeyword
        );
    }

    public RevenueStatisticsResponse getRevenueStatistics(
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            CurrentUser currentUser
    ) {
        ensureStatisticsReader(currentUser);
        FilterRange range = normalizeRange(startDate, endDate);
        String likeKeyword = normalizeLikeKeyword(keyword);

        List<RevenueDetailRecord> details = jdbcTemplate.query(
                """
                SELECT
                    pr.id AS payment_id,
                    c.course_name,
                    s.full_name AS student_name,
                    pr.receivable_amount,
                    pr.paid_amount,
                    COALESCE(pr.payment_method, '未收费') AS payment_method,
                    pr.paid_at,
                    COALESCE(u.display_name, '') AS handled_by_name
                FROM payment_record pr
                INNER JOIN course c ON pr.course_id = c.id
                INNER JOIN student_profile s ON pr.student_id = s.id
                LEFT JOIN user_account u ON pr.handled_by = u.id
                WHERE c.start_time >= ?
                  AND c.start_time < ?
                  AND (
                    ? IS NULL
                    OR c.course_name LIKE ?
                    OR s.full_name LIKE ?
                  )
                ORDER BY COALESCE(pr.paid_at, pr.updated_at) DESC, pr.id DESC
                """,
                REVENUE_DETAIL_ROW_MAPPER,
                range.startAt(), range.endExclusive(),
                likeKeyword, likeKeyword, likeKeyword
        );

        BigDecimal receivableAmountTotal = BigDecimal.ZERO;
        BigDecimal paidAmountTotal = BigDecimal.ZERO;
        BigDecimal cashAmount = BigDecimal.ZERO;
        BigDecimal transferAmount = BigDecimal.ZERO;
        int specialPaymentCount = 0;

        for (RevenueDetailRecord detail : details) {
            receivableAmountTotal = receivableAmountTotal.add(nvl(detail.receivableAmount()));
            paidAmountTotal = paidAmountTotal.add(nvl(detail.paidAmount()));
            if ("CASH".equals(detail.paymentMethod())) {
                cashAmount = cashAmount.add(nvl(detail.paidAmount()));
            } else if ("TRANSFER".equals(detail.paymentMethod())) {
                transferAmount = transferAmount.add(nvl(detail.paidAmount()));
            } else if ("CORPORATE".equals(detail.paymentMethod()) || "WAIVED".equals(detail.paymentMethod())) {
                specialPaymentCount++;
            }
        }

        BigDecimal ratioBase = cashAmount.add(transferAmount);
        BigDecimal cashAmountRatio = calculateRatio(cashAmount, ratioBase);
        BigDecimal transferAmountRatio = calculateRatio(transferAmount, ratioBase);

        return new RevenueStatisticsResponse(
                receivableAmountTotal.setScale(2, RoundingMode.HALF_UP),
                paidAmountTotal.setScale(2, RoundingMode.HALF_UP),
                specialPaymentCount,
                cashAmountRatio,
                transferAmountRatio,
                details
        );
    }

    private void ensureStatisticsReader(CurrentUser currentUser) {
        if (currentUser.hasRole("MANAGER") || currentUser.hasRole("EXECUTOR")) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前角色无权查看统计报表");
    }

    private FilterRange normalizeRange(LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveEndDate = endDate == null ? LocalDate.now() : endDate;
        LocalDate effectiveStartDate = startDate == null ? effectiveEndDate.minusMonths(3) : startDate;
        if (effectiveStartDate.isAfter(effectiveEndDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "开始日期不能晚于结束日期");
        }
        return new FilterRange(
                Timestamp.valueOf(effectiveStartDate.atStartOfDay()),
                Timestamp.valueOf(effectiveEndDate.plusDays(1).atStartOfDay())
        );
    }

    private String normalizeLikeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim() + "%";
    }

    private BigDecimal calculateRatio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record FilterRange(
            Timestamp startAt,
            Timestamp endExclusive
    ) {
    }
}
