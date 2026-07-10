package com.hqtraining.backend.controller;

import com.hqtraining.backend.common.ApiResponse;
import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.EnrollmentConfirmRequest;
import com.hqtraining.backend.dto.EnrollmentCreateRequest;
import com.hqtraining.backend.model.CourseOptionRecord;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.EnrollmentRecord;
import com.hqtraining.backend.model.StudentOptionRecord;
import com.hqtraining.backend.service.AuthService;
import com.hqtraining.backend.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final AuthService authService;

    public EnrollmentController(EnrollmentService enrollmentService, AuthService authService) {
        this.enrollmentService = enrollmentService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<PageResult<EnrollmentRecord>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long studentId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(
                enrollmentService.getEnrollments(pageNum, pageSize, keyword, status, courseId, studentId, currentUser)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<EnrollmentRecord> detail(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(enrollmentService.getEnrollmentById(id, currentUser));
    }

    @PostMapping
    public ApiResponse<EnrollmentRecord> create(
            @Valid @RequestBody EnrollmentCreateRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(enrollmentService.createEnrollment(request, currentUser));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<EnrollmentRecord> confirm(
            @PathVariable Long id,
            @Valid @RequestBody EnrollmentConfirmRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(enrollmentService.confirmEnrollment(id, request, currentUser));
    }

    @GetMapping("/options/courses")
    public ApiResponse<List<CourseOptionRecord>> courseOptions(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(enrollmentService.getCourseOptions(currentUser));
    }

    @GetMapping("/options/students")
    public ApiResponse<List<StudentOptionRecord>> studentOptions(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(enrollmentService.getStudentOptions(currentUser));
    }
}
