package com.hqtraining.backend.controller;

import com.hqtraining.backend.common.ApiResponse;
import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.StudentSaveRequest;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.StudentProfileRecord;
import com.hqtraining.backend.service.AuthService;
import com.hqtraining.backend.service.StudentService;
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
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final AuthService authService;

    public StudentController(StudentService studentService, AuthService authService) {
        this.studentService = studentService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<PageResult<StudentProfileRecord>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long companyId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(studentService.getStudents(pageNum, pageSize, keyword, companyId, currentUser));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentProfileRecord> detail(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(studentService.getStudentById(id, currentUser));
    }

    @PostMapping
    public ApiResponse<StudentProfileRecord> create(
            @Valid @RequestBody StudentSaveRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(studentService.createStudent(request, currentUser));
    }

    @PutMapping("/{id}")
    public ApiResponse<StudentProfileRecord> update(
            @PathVariable Long id,
            @Valid @RequestBody StudentSaveRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(studentService.updateStudent(id, request, currentUser));
    }
}
