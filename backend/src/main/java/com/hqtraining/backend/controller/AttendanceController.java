package com.hqtraining.backend.controller;

import com.hqtraining.backend.common.ApiResponse;
import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.AttendanceCheckInRequest;
import com.hqtraining.backend.model.AttendanceRecordView;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.service.AuthService;
import com.hqtraining.backend.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance-records")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AuthService authService;

    public AttendanceController(AttendanceService attendanceService, AuthService authService) {
        this.attendanceService = attendanceService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<PageResult<AttendanceRecordView>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long courseId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(
                attendanceService.getAttendanceRecords(pageNum, pageSize, keyword, status, courseId, currentUser)
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<AttendanceRecordView> detail(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(attendanceService.getAttendanceRecordById(id, currentUser));
    }

    @PostMapping("/{id}/check-in")
    public ApiResponse<AttendanceRecordView> checkIn(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceCheckInRequest request,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(attendanceService.checkIn(id, request, currentUser));
    }
}
