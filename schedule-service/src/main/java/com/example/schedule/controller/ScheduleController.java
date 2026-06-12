package com.example.schedule.controller;

import com.example.schedule.dto.ScheduleRequest;
import com.example.schedule.dto.ScheduleResponse;
import com.example.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers/{providerId}/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules", description = "Manage available time slots for providers")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/available")
    @Operation(summary = "List future available slots for a provider")
    public ResponseEntity<List<ScheduleResponse>> findAvailable(@PathVariable Long providerId) {
        return ResponseEntity.ok(scheduleService.findAvailableByProvider(providerId));
    }

    @GetMapping
    @Operation(summary = "List all schedule slots for a provider (including booked ones)")
    public ResponseEntity<List<ScheduleResponse>> findAll(@PathVariable Long providerId) {
        return ResponseEntity.ok(scheduleService.findAllByProvider(providerId));
    }

    @PostMapping
    @Operation(summary = "Add a single available time slot for a provider")
    public ResponseEntity<ScheduleResponse> create(
            @PathVariable Long providerId,
            @Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(providerId, request));
    }

    @PostMapping("/batch")
    @Operation(summary = "Add multiple available time slots at once")
    public ResponseEntity<List<ScheduleResponse>> createBatch(
            @PathVariable Long providerId,
            @Valid @RequestBody List<ScheduleRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createBatch(providerId, requests));
    }

    @DeleteMapping("/{scheduleId}")
    @Operation(summary = "Remove an available slot (only allowed if not yet booked)")
    public ResponseEntity<Void> delete(@PathVariable Long scheduleId) {
        scheduleService.delete(scheduleId);
        return ResponseEntity.noContent().build();
    }
}
