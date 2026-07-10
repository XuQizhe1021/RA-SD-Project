package com.hqtraining.backend.controller;

import com.hqtraining.backend.common.ApiResponse;
import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.LecturerSaveRequest;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.LecturerRecord;
import com.hqtraining.backend.service.AuthService;
import com.hqtraining.backend.service.LecturerService;
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

import java.util.List;

@RestController
@RequestMapping("/api/lecturers")
public class LecturerController {

    private final LecturerService lecturerService;
    private final AuthService authService;

    public LecturerController(LecturerService lecturerService, AuthService authService) {
        this.lecturerService = lecturerService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<PageResult<LecturerRecord>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(lecturerService.getLecturers(pageNum, pageSize, keyword, status, currentUser));
    }

    @GetMapping("/options")
    public ApiResponse<List<LecturerRecord>> options(@RequestHeader("Authorization") String authorizationHeader) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(lecturerService.getActiveLecturers(currentUser));
    }

    @GetMapping("/{id}")
    public ApiResponse<LecturerRecord> detail(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(lecturerService.getLecturerById(id, currentUser));
    }

    @PostMapping
    public ApiResponse<LecturerRecord> create(
            @Valid @RequestBody LecturerSaveRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(lecturerService.createLecturer(request, currentUser));
    }

    @PutMapping("/{id}")
    public ApiResponse<LecturerRecord> update(
            @PathVariable Long id,
            @Valid @RequestBody LecturerSaveRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(lecturerService.updateLecturer(id, request, currentUser));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<LecturerRecord> disable(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(lecturerService.disableLecturer(id, currentUser));
    }
}
