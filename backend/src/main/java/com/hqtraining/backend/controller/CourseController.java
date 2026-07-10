package com.hqtraining.backend.controller;

import com.hqtraining.backend.common.ApiResponse;
import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.CourseSaveRequest;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.CourseRecord;
import com.hqtraining.backend.service.AuthService;
import com.hqtraining.backend.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final AuthService authService;

    public CourseController(CourseService courseService, AuthService authService) {
        this.courseService = courseService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<PageResult<CourseRecord>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long lecturerId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(courseService.getCourses(pageNum, pageSize, keyword, status, lecturerId, currentUser));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseRecord> detail(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(courseService.getCourseById(id, currentUser));
    }

    @PostMapping
    public ApiResponse<CourseRecord> create(
            @Valid @RequestBody CourseSaveRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(courseService.createCourse(request, currentUser));
    }

    @PutMapping("/{id}")
    public ApiResponse<CourseRecord> update(
            @PathVariable Long id,
            @Valid @RequestBody CourseSaveRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(courseService.updateCourse(id, request, currentUser));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<CourseRecord> publish(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(courseService.publishCourse(id, currentUser));
    }
}
