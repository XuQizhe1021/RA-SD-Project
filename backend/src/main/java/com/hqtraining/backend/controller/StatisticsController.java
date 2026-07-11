package com.hqtraining.backend.controller;

import com.hqtraining.backend.common.ApiResponse;
import com.hqtraining.backend.model.CourseStatisticsRecord;
import com.hqtraining.backend.model.CurrentUser;
import com.hqtraining.backend.model.LecturerStatisticsRecord;
import com.hqtraining.backend.model.RevenueStatisticsResponse;
import com.hqtraining.backend.model.StudentStatisticsRecord;
import com.hqtraining.backend.service.AuthService;
import com.hqtraining.backend.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final AuthService authService;

    public StatisticsController(StatisticsService statisticsService, AuthService authService) {
        this.statisticsService = statisticsService;
        this.authService = authService;
    }

    @GetMapping("/courses")
    public ApiResponse<List<CourseStatisticsRecord>> courses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(statisticsService.getCourseStatistics(startDate, endDate, keyword, currentUser));
    }

    @GetMapping("/students")
    public ApiResponse<List<StudentStatisticsRecord>> students(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(statisticsService.getStudentStatistics(startDate, endDate, keyword, currentUser));
    }

    @GetMapping("/lecturers")
    public ApiResponse<List<LecturerStatisticsRecord>> lecturers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(statisticsService.getLecturerStatistics(startDate, endDate, keyword, currentUser));
    }

    @GetMapping("/revenue")
    public ApiResponse<RevenueStatisticsResponse> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        CurrentUser currentUser = authService.requireCurrentUser(authorizationHeader);
        return ApiResponse.success(statisticsService.getRevenueStatistics(startDate, endDate, keyword, currentUser));
    }
}
