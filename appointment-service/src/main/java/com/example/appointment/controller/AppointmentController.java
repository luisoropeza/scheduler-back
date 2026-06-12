package com.example.appointment.controller;

import com.example.appointment.dto.AppointmentRequest;
import com.example.appointment.dto.AppointmentResponse;
import com.example.appointment.entity.enums.AppointmentStatus;
import com.example.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Book, confirm, and cancel appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Book an appointment — called by n8n after the client selects a slot")
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.book(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment details by ID")
    public ResponseEntity<AppointmentResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.findById(id));
    }

    @GetMapping
    @Operation(summary = "List appointments by client phone number — key endpoint for WhatsApp flow")
    public ResponseEntity<List<AppointmentResponse>> findByClientPhone(@RequestParam String phone) {
        return ResponseEntity.ok(appointmentService.findByClientPhone(phone));
    }

    @GetMapping("/provider/{providerId}")
    @Operation(summary = "List appointments for a provider, optionally filtered by status")
    public ResponseEntity<List<AppointmentResponse>> findByProvider(
            @PathVariable Long providerId,
            @RequestParam(required = false, defaultValue = "CONFIRMED") AppointmentStatus status) {
        return ResponseEntity.ok(appointmentService.findByProviderAndStatus(providerId, status));
    }

    @PatchMapping("/{id}/confirm")
    @Operation(summary = "Confirm a pending appointment")
    public ResponseEntity<AppointmentResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.confirm(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an appointment and release the time slot back to available")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancel(id));
    }
}
