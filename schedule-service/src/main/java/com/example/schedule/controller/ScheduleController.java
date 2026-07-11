package com.example.schedule.controller;

import com.example.schedule.dto.ScheduleRequest;
import com.example.schedule.dto.ScheduleResponse;
import com.example.schedule.enums.ScheduleStatus;
import com.example.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "Manage and browse time slots")
public class ScheduleController {
    private final ScheduleService scheduleService;

    @GetMapping("/api/schedules")
    @Operation(summary = "GET /api/schedules — browse slots, filter by ?doctorId={doctorId}?specialty={specialty}?status={status}?after={after}")
    public ResponseEntity<Page<ScheduleResponse>> findAvailable(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) ScheduleStatus status,
            @RequestParam(required = false) LocalDateTime after,
            @PageableDefault(sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(scheduleService.findAll(doctorId, specialty, status, after, pageable));
    }

    @GetMapping("/api/schedules/{id}")
    @Operation(summary = "GET /api/schedules/{id} — get a schedule slot by ID")
    public ResponseEntity<ScheduleResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getById(id));
    }

    @PostMapping("/api/personal/{doctorId}/schedules")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "POST /api/personal/{doctorId}/schedules — add a single available time slot for a doctor")
    public ResponseEntity<ScheduleResponse> create(
            @PathVariable Long doctorId,
            @Valid @RequestBody ScheduleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(doctorId, request));
    }

    @PostMapping("/api/personal/{doctorId}/schedules/batch")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "POST /api/personal/{doctorId}/schedules/batch — add multiple available time slots for a doctor")
    public ResponseEntity<List<ScheduleResponse>> createBatch(
            @PathVariable Long doctorId,
            @Valid @RequestBody List<ScheduleRequest> requests
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createBatch(doctorId, requests));
    }

    @DeleteMapping("/api/personal/{doctorId}/schedules/{scheduleId}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "DELETE /api/personal/{doctorId}/schedules/{scheduleId} — remove an available slot")
    public ResponseEntity<Void> delete(
            @PathVariable Long doctorId,
            @PathVariable Long scheduleId
    ) {
        scheduleService.delete(scheduleId, doctorId);
        return ResponseEntity.noContent().build();
    }
}
