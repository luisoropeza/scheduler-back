package com.example.schedule.controller;

import com.example.schedule.dto.ScheduleResponse;
import com.example.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal endpoints consumed only by appointment-service (not exposed via the nginx gateway).
 */
@RestController
@RequestMapping("/internal/schedules")
@RequiredArgsConstructor
public class ScheduleInternalController {

    private final ScheduleService scheduleService;

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.getById(id));
    }

    @PatchMapping("/{id}/book")
    public ResponseEntity<ScheduleResponse> book(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.book(id));
    }

    @PatchMapping("/{id}/release")
    public ResponseEntity<ScheduleResponse> release(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleService.release(id));
    }
}
