package com.hqtraining.backend.service;

import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.LecturerSaveRequest;
import com.hqtraining.backend.model.LecturerRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LecturerService {

    private final CopyOnWriteArrayList<LecturerRecord> lecturers = new CopyOnWriteArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1000);

    public LecturerService() {
        lecturers.add(new LecturerRecord(
                1L,
                "LEC20260708001",
                "周教授",
                "高级架构师",
                "微服务架构, Java企业级开发",
                "13810000001",
                "zhou.teacher@hq.local",
                new BigDecimal("6800.00"),
                "负责企业级架构与后端技术培训。",
                "ACTIVE",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1)
        ));
        lecturers.add(new LecturerRecord(
                2L,
                "LEC20260708002",
                "陈老师",
                "敏捷教练",
                "Scrum实践, 项目管理",
                "13810000002",
                "chen.agile@hq.local",
                new BigDecimal("5200.00"),
                "擅长敏捷研发流程与项目复盘。",
                "ACTIVE",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusHours(20)
        ));
        idGenerator.set(2);
    }

    public PageResult<LecturerRecord> getLecturers(int pageNum, int pageSize, String keyword, String status) {
        List<LecturerRecord> filtered = lecturers.stream()
                .filter(item -> matchesKeyword(item, keyword))
                .filter(item -> matchesStatus(item, status))
                .sorted(Comparator.comparing(LecturerRecord::updatedAt).reversed())
                .toList();

        return toPage(filtered, pageNum, pageSize);
    }

    public List<LecturerRecord> getActiveLecturers() {
        return lecturers.stream()
                .filter(item -> "ACTIVE".equals(item.status()))
                .sorted(Comparator.comparing(LecturerRecord::fullName))
                .toList();
    }

    public LecturerRecord getLecturerById(Long id) {
        return lecturers.stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "讲师不存在"));
    }

    public LecturerRecord createLecturer(LecturerSaveRequest request) {
        long id = idGenerator.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();
        LecturerRecord record = new LecturerRecord(
                id,
                generateLecturerNo(id),
                request.fullName().trim(),
                request.title(),
                request.specialty(),
                request.phone(),
                request.email(),
                request.feeStandard() == null ? BigDecimal.ZERO : request.feeStandard(),
                request.profileText(),
                "ACTIVE",
                now,
                now
        );
        lecturers.add(record);
        return record;
    }

    public LecturerRecord updateLecturer(Long id, LecturerSaveRequest request) {
        LecturerRecord existing = getLecturerById(id);
        LecturerRecord updated = new LecturerRecord(
                existing.id(),
                existing.lecturerNo(),
                request.fullName().trim(),
                request.title(),
                request.specialty(),
                request.phone(),
                request.email(),
                request.feeStandard() == null ? BigDecimal.ZERO : request.feeStandard(),
                request.profileText(),
                existing.status(),
                existing.createdAt(),
                LocalDateTime.now()
        );
        replace(existing, updated);
        return updated;
    }

    public LecturerRecord disableLecturer(Long id) {
        LecturerRecord existing = getLecturerById(id);
        LecturerRecord updated = new LecturerRecord(
                existing.id(),
                existing.lecturerNo(),
                existing.fullName(),
                existing.title(),
                existing.specialty(),
                existing.phone(),
                existing.email(),
                existing.feeStandard(),
                existing.profileText(),
                "DISABLED",
                existing.createdAt(),
                LocalDateTime.now()
        );
        replace(existing, updated);
        return updated;
    }

    private boolean matchesKeyword(LecturerRecord item, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return item.fullName().toLowerCase(Locale.ROOT).contains(normalized)
                || item.lecturerNo().toLowerCase(Locale.ROOT).contains(normalized)
                || safe(item.title()).toLowerCase(Locale.ROOT).contains(normalized)
                || safe(item.specialty()).toLowerCase(Locale.ROOT).contains(normalized);
    }

    private boolean matchesStatus(LecturerRecord item, String status) {
        return status == null || status.isBlank() || item.status().equalsIgnoreCase(status);
    }

    private PageResult<LecturerRecord> toPage(List<LecturerRecord> records, int pageNum, int pageSize) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = (safePageNum - 1) * safePageSize;
        if (fromIndex >= records.size()) {
            return new PageResult<>(List.of(), safePageNum, safePageSize, records.size());
        }

        int toIndex = Math.min(fromIndex + safePageSize, records.size());
        return new PageResult<>(new ArrayList<>(records.subList(fromIndex, toIndex)), safePageNum, safePageSize, records.size());
    }

    private void replace(LecturerRecord existing, LecturerRecord updated) {
        int index = lecturers.indexOf(existing);
        if (index < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "讲师不存在");
        }
        lecturers.set(index, updated);
    }

    private String generateLecturerNo(long id) {
        return "LEC20260708%03d".formatted(id);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
