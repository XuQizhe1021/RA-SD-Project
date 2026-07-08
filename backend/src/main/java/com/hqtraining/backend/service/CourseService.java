package com.hqtraining.backend.service;

import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.CourseSaveRequest;
import com.hqtraining.backend.model.CourseRecord;
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
public class CourseService {

    private final CopyOnWriteArrayList<CourseRecord> courses = new CopyOnWriteArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(2000);
    private final LecturerService lecturerService;

    public CourseService(LecturerService lecturerService) {
        this.lecturerService = lecturerService;
        courses.add(new CourseRecord(
                1L,
                "CRS20260708001",
                1L,
                "Spring Boot 企业级开发实战",
                1L,
                "周教授",
                2L,
                "执行人-李工",
                LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(1).withHour(17).withMinute(30).withSecond(0).withNano(0),
                "未来技术学院 A301",
                60,
                new BigDecimal("1999.00"),
                "DRAFT",
                "SYSTEM",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusHours(8)
        ));
        courses.add(new CourseRecord(
                2L,
                "CRS20260708002",
                2L,
                "Scrum 冲刺管理与实践",
                2L,
                "陈老师",
                2L,
                "执行人-李工",
                LocalDateTime.now().plusDays(2).withHour(13).withMinute(30).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(2).withHour(18).withMinute(0).withSecond(0).withNano(0),
                "未来技术学院 B201",
                45,
                new BigDecimal("1299.00"),
                "PUBLISHED",
                "SYSTEM",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusHours(3)
        ));
        idGenerator.set(2);
    }

    public PageResult<CourseRecord> getCourses(
            int pageNum,
            int pageSize,
            String keyword,
            String status,
            Long lecturerId
    ) {
        List<CourseRecord> filtered = courses.stream()
                .filter(item -> matchesKeyword(item, keyword))
                .filter(item -> matchesStatus(item, status))
                .filter(item -> lecturerId == null || lecturerId.equals(item.lecturerId()))
                .sorted(Comparator.comparing(CourseRecord::startTime))
                .toList();

        return toPage(filtered, pageNum, pageSize);
    }

    public CourseRecord getCourseById(Long id) {
        return courses.stream()
                .filter(item -> item.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在"));
    }

    public CourseRecord createCourse(CourseSaveRequest request) {
        validateCourseRequest(request);
        long id = idGenerator.incrementAndGet();
        LocalDateTime now = LocalDateTime.now();
        LecturerRecord lecturer = resolveLecturer(request.lecturerId());

        CourseRecord record = new CourseRecord(
                id,
                generateCourseNo(id),
                request.applicationId(),
                request.courseName().trim(),
                lecturer == null ? null : lecturer.id(),
                lecturer == null ? "" : lecturer.fullName(),
                2L,
                "执行人-李工",
                request.startTime(),
                request.endTime(),
                request.location().trim(),
                request.quota(),
                request.feeAmount() == null ? BigDecimal.ZERO : request.feeAmount(),
                "DRAFT",
                "SYSTEM",
                now,
                now
        );
        courses.add(record);
        return record;
    }

    public CourseRecord updateCourse(Long id, CourseSaveRequest request) {
        validateCourseRequest(request);
        CourseRecord existing = getCourseById(id);
        LecturerRecord lecturer = resolveLecturer(request.lecturerId());

        CourseRecord updated = new CourseRecord(
                existing.id(),
                existing.courseNo(),
                request.applicationId(),
                request.courseName().trim(),
                lecturer == null ? null : lecturer.id(),
                lecturer == null ? "" : lecturer.fullName(),
                existing.executorUserId(),
                existing.executorName(),
                request.startTime(),
                request.endTime(),
                request.location().trim(),
                request.quota(),
                request.feeAmount() == null ? BigDecimal.ZERO : request.feeAmount(),
                existing.status(),
                existing.sourceType(),
                existing.createdAt(),
                LocalDateTime.now()
        );
        replace(existing, updated);
        return updated;
    }

    public CourseRecord publishCourse(Long id) {
        CourseRecord existing = getCourseById(id);
        if (!"DRAFT".equals(existing.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只有草稿状态的课程可以发布");
        }
        CourseRecord updated = new CourseRecord(
                existing.id(),
                existing.courseNo(),
                existing.applicationId(),
                existing.courseName(),
                existing.lecturerId(),
                existing.lecturerName(),
                existing.executorUserId(),
                existing.executorName(),
                existing.startTime(),
                existing.endTime(),
                existing.location(),
                existing.quota(),
                existing.feeAmount(),
                "PUBLISHED",
                existing.sourceType(),
                existing.createdAt(),
                LocalDateTime.now()
        );
        replace(existing, updated);
        return updated;
    }

    private void validateCourseRequest(CourseSaveRequest request) {
        if (request.endTime().isBefore(request.startTime()) || request.endTime().isEqual(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
    }

    private LecturerRecord resolveLecturer(Long lecturerId) {
        if (lecturerId == null) {
            return null;
        }
        LecturerRecord lecturer = lecturerService.getLecturerById(lecturerId);
        if (!"ACTIVE".equals(lecturer.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只能选择启用中的讲师");
        }
        return lecturer;
    }

    private boolean matchesKeyword(CourseRecord item, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return item.courseName().toLowerCase(Locale.ROOT).contains(normalized)
                || item.courseNo().toLowerCase(Locale.ROOT).contains(normalized)
                || item.location().toLowerCase(Locale.ROOT).contains(normalized)
                || item.lecturerName().toLowerCase(Locale.ROOT).contains(normalized);
    }

    private boolean matchesStatus(CourseRecord item, String status) {
        return status == null || status.isBlank() || item.status().equalsIgnoreCase(status);
    }

    private PageResult<CourseRecord> toPage(List<CourseRecord> records, int pageNum, int pageSize) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.max(pageSize, 1);
        int fromIndex = (safePageNum - 1) * safePageSize;
        if (fromIndex >= records.size()) {
            return new PageResult<>(List.of(), safePageNum, safePageSize, records.size());
        }

        int toIndex = Math.min(fromIndex + safePageSize, records.size());
        return new PageResult<>(new ArrayList<>(records.subList(fromIndex, toIndex)), safePageNum, safePageSize, records.size());
    }

    private void replace(CourseRecord existing, CourseRecord updated) {
        int index = courses.indexOf(existing);
        if (index < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在");
        }
        courses.set(index, updated);
    }

    private String generateCourseNo(long id) {
        return "CRS20260708%03d".formatted(id);
    }
}
