package com.example.schedule.controller;

import com.example.schedule.dto.ScheduleResponse;
import com.example.schedule.enums.ScheduleStatus;
import com.example.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Read-only browsing facade for automated callers (currently the n8n WhatsApp workflow).
 * Authenticated via a static API key ({@link com.example.schedule.security.ApiKeyAuthFilter}).
 */
@RestController
@RequestMapping("/api/integrations/n8n")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTEGRATION')")
@Tag(name = "Integrations", description = "Facade for automated booking agents (n8n)")
public class IntegrationController {
    private final ScheduleService scheduleService;

    @GetMapping("/schedules")
    @Operation(summary = "GET /api/integrations/n8n/schedules — list available slots for ?doctorId={doctorId}")
    public ResponseEntity<Page<ScheduleResponse>> findAvailableSchedules(
            @RequestParam Long doctorId,
            @PageableDefault(sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(scheduleService.findAll(doctorId, null, ScheduleStatus.AVAILABLE, LocalDateTime.now(), pageable));
    }
}
