package com.example.appointment.controller;

import com.example.appointment.dto.AppointmentRequest;
import com.example.appointment.dto.AppointmentResponse;
import com.example.appointment.enums.AppointmentStatus;
import com.example.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Book, confirm, cancel, and reschedule appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "POST /api/appointments — book an appointment for a patient on a given schedule slot")
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.book(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "GET /api/appointments/{id} — get appointment details by ID")
    public ResponseEntity<AppointmentResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.findById(id));
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "GET /api/appointments/client/{clientId} — list all appointments for a patient")
    public ResponseEntity<Page<AppointmentResponse>> findByClient(
            @PathVariable Long clientId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(appointmentService.findByClientId(clientId, pageable));
    }

    @GetMapping("/personal/{personalId}")
    @Operation(summary = "GET /api/appointments/personal/{personalId} — list appointments for a personal, filter by ?status=PENDING|CONFIRMED|CANCELLED")
    public ResponseEntity<Page<AppointmentResponse>> findByPersonal(
            @PathVariable Long personalId,
            @RequestParam(required = false) AppointmentStatus status,
            @PageableDefault(sort = "scheduleStart", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(appointmentService.findByDoctorAndStatus(personalId, status, pageable));
    }

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "PATCH /api/appointments/{id}/confirm — confirm a pending appointment (doctor only)")
    public ResponseEntity<AppointmentResponse> confirm(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(appointmentService.confirm(id, Long.parseLong(auth.getName())));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "PATCH /api/appointments/{id}/cancel — cancel an appointment (doctor or patient)")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(appointmentService.cancel(id, Long.parseLong(auth.getName())));
    }

    @Data
    public static class RescheduleRequest { private Long scheduleId; }

    @PatchMapping("/{id}/reschedule")
    @Operation(summary = "PATCH /api/appointments/{id}/reschedule — move an appointment to a new schedule slot (body: {scheduleId})")
    public ResponseEntity<AppointmentResponse> reschedule(
            @PathVariable Long id,
            @RequestBody RescheduleRequest body,
            Authentication auth) {
        return ResponseEntity.ok(appointmentService.reschedule(id, body.getScheduleId(), Long.parseLong(auth.getName())));
    }
}
