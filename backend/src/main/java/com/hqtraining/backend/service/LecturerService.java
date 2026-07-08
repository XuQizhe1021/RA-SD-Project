package com.hqtraining.backend.service;

import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.LecturerSaveRequest;
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
public class LecturerService {

    private static final RowMapper<LecturerRecord> LECTURER_ROW_MAPPER = (rs, rowNum) -> new LecturerRecord(
            rs.getLong("id"),
            rs.getString("lecturer_no"),
            rs.getString("full_name"),
            rs.getString("title"),
            rs.getString("specialty"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getBigDecimal("fee_standard"),
            rs.getString("profile_text"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public LecturerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<LecturerRecord> getLecturers(int pageNum, int pageSize, String keyword, String status) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        String likeKeyword = normalizeLikeKeyword(keyword);

        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM lecturer_profile
                WHERE (? IS NULL OR status = ?)
                  AND (
                    ? IS NULL
                    OR lecturer_no LIKE ?
                    OR full_name LIKE ?
                    OR COALESCE(title, '') LIKE ?
                    OR COALESCE(specialty, '') LIKE ?
                  )
                """,
                Long.class,
                emptyToNull(status), emptyToNull(status),
                likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword
        );

        List<LecturerRecord> list = jdbcTemplate.query(
                """
                SELECT id, lecturer_no, full_name, title, specialty, phone, email, fee_standard, profile_text, status, created_at, updated_at
                FROM lecturer_profile
                WHERE (? IS NULL OR status = ?)
                  AND (
                    ? IS NULL
                    OR lecturer_no LIKE ?
                    OR full_name LIKE ?
                    OR COALESCE(title, '') LIKE ?
                    OR COALESCE(specialty, '') LIKE ?
                  )
                ORDER BY updated_at DESC
                LIMIT ? OFFSET ?
                """,
                LECTURER_ROW_MAPPER,
                emptyToNull(status), emptyToNull(status),
                likeKeyword, likeKeyword, likeKeyword, likeKeyword, likeKeyword,
                safePageSize, (safePageNum - 1) * safePageSize
        );

        return new PageResult<>(list, safePageNum, safePageSize, total == null ? 0 : total);
    }

    public List<LecturerRecord> getActiveLecturers() {
        return jdbcTemplate.query(
                """
                SELECT id, lecturer_no, full_name, title, specialty, phone, email, fee_standard, profile_text, status, created_at, updated_at
                FROM lecturer_profile
                WHERE status = 'ACTIVE'
                ORDER BY full_name ASC
                """,
                LECTURER_ROW_MAPPER
        );
    }

    public LecturerRecord getLecturerById(Long id) {
        List<LecturerRecord> lecturers = jdbcTemplate.query(
                """
                SELECT id, lecturer_no, full_name, title, specialty, phone, email, fee_standard, profile_text, status, created_at, updated_at
                FROM lecturer_profile
                WHERE id = ?
                """,
                LECTURER_ROW_MAPPER,
                id
        );
        if (lecturers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "讲师不存在");
        }
        return lecturers.get(0);
    }

    public LecturerRecord createLecturer(LecturerSaveRequest request) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO lecturer_profile (
                        lecturer_no, full_name, title, specialty, phone, email, fee_standard, profile_text, status, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, generateLecturerNo());
            statement.setString(2, request.fullName().trim());
            statement.setString(3, emptyToNull(request.title()));
            statement.setString(4, emptyToNull(request.specialty()));
            statement.setString(5, emptyToNull(request.phone()));
            statement.setString(6, emptyToNull(request.email()));
            statement.setBigDecimal(7, request.feeStandard() == null ? BigDecimal.ZERO : request.feeStandard());
            statement.setString(8, emptyToNull(request.profileText()));
            statement.setTimestamp(9, Timestamp.valueOf(now));
            statement.setTimestamp(10, Timestamp.valueOf(now));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "讲师创建失败");
        }
        return getLecturerById(key.longValue());
    }

    public LecturerRecord updateLecturer(Long id, LecturerSaveRequest request) {
        LecturerRecord existing = getLecturerById(id);
        int updatedRows = jdbcTemplate.update(
                """
                UPDATE lecturer_profile
                SET full_name = ?, title = ?, specialty = ?, phone = ?, email = ?, fee_standard = ?, profile_text = ?, updated_at = ?
                WHERE id = ?
                """,
                request.fullName().trim(),
                emptyToNull(request.title()),
                emptyToNull(request.specialty()),
                emptyToNull(request.phone()),
                emptyToNull(request.email()),
                request.feeStandard() == null ? BigDecimal.ZERO : request.feeStandard(),
                emptyToNull(request.profileText()),
                Timestamp.valueOf(LocalDateTime.now()),
                existing.id()
        );
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "讲师不存在");
        }
        return getLecturerById(id);
    }

    public LecturerRecord disableLecturer(Long id) {
        int updatedRows = jdbcTemplate.update(
                """
                UPDATE lecturer_profile
                SET status = 'DISABLED', updated_at = ?
                WHERE id = ?
                """,
                Timestamp.valueOf(LocalDateTime.now()),
                id
        );
        if (updatedRows == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "讲师不存在");
        }
        return getLecturerById(id);
    }

    private String generateLecturerNo() {
        Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM lecturer_profile", Long.class);
        long nextId = (maxId == null ? 0 : maxId) + 1;
        return "LEC20260708%03d".formatted(nextId);
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
