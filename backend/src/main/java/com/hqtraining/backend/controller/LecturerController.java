package com.hqtraining.backend.controller;

import com.hqtraining.backend.common.ApiResponse;
import com.hqtraining.backend.common.PageResult;
import com.hqtraining.backend.dto.LecturerSaveRequest;
import com.hqtraining.backend.model.LecturerRecord;
import com.hqtraining.backend.service.LecturerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lecturers")
public class LecturerController {

    private final LecturerService lecturerService;

    public LecturerController(LecturerService lecturerService) {
        this.lecturerService = lecturerService;
    }

    @GetMapping
    public ApiResponse<PageResult<LecturerRecord>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(lecturerService.getLecturers(pageNum, pageSize, keyword, status));
    }

    @GetMapping("/options")
    public ApiResponse<List<LecturerRecord>> options() {
        return ApiResponse.success(lecturerService.getActiveLecturers());
    }

    @GetMapping("/{id}")
    public ApiResponse<LecturerRecord> detail(@PathVariable Long id) {
        return ApiResponse.success(lecturerService.getLecturerById(id));
    }

    @PostMapping
    public ApiResponse<LecturerRecord> create(@Valid @RequestBody LecturerSaveRequest request) {
        return ApiResponse.success(lecturerService.createLecturer(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<LecturerRecord> update(@PathVariable Long id, @Valid @RequestBody LecturerSaveRequest request) {
        return ApiResponse.success(lecturerService.updateLecturer(id, request));
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<LecturerRecord> disable(@PathVariable Long id) {
        return ApiResponse.success(lecturerService.disableLecturer(id));
    }
}
