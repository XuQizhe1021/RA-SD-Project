package com.hqtraining.backend.controller;

import com.hqtraining.backend.common.ApiResponse;
import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.CourseSaveRequest;
import com.hqtraining.backend.model.CourseRecord;
import com.hqtraining.backend.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ApiResponse<PageResult<CourseRecord>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long lecturerId
    ) {
        return ApiResponse.success(courseService.getCourses(pageNum, pageSize, keyword, status, lecturerId));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseRecord> detail(@PathVariable Long id) {
        return ApiResponse.success(courseService.getCourseById(id));
    }

    @PostMapping
    public ApiResponse<CourseRecord> create(@Valid @RequestBody CourseSaveRequest request) {
        return ApiResponse.success(courseService.createCourse(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CourseRecord> update(@PathVariable Long id, @Valid @RequestBody CourseSaveRequest request) {
        return ApiResponse.success(courseService.updateCourse(id, request));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<CourseRecord> publish(@PathVariable Long id) {
        return ApiResponse.success(courseService.publishCourse(id));
    }
}
