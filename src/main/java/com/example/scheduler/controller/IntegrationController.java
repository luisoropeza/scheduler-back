package com.example.scheduler.controller;

import com.example.scheduler.dto.AppointmentRequest;
import com.example.scheduler.dto.AppointmentResponse;
import com.example.scheduler.dto.PatientResponse;
import com.example.scheduler.dto.PersonalResponse;
import com.example.scheduler.dto.ScheduleResponse;
import com.example.scheduler.dto.SpecialtyResponse;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.enums.ScheduleStatus;
import com.example.scheduler.middleware.ApiKeyAuthFilter;
import com.example.scheduler.service.AppointmentService;
import com.example.scheduler.service.PatientService;
import com.example.scheduler.service.PersonalService;
import com.example.scheduler.service.ScheduleService;
import com.example.scheduler.service.SpecialtyService;
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

/**
 * Read-only browsing + booking facade for automated callers (currently the n8n WhatsApp
 * workflow). Authenticated via a static API key ({@link ApiKeyAuthFilter}),
 * not a per-patient JWT, since the caller only knows the patient's phone number.
 */
@RestController
@RequestMapping("/api/integrations/n8n")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTEGRATION')")
@Tag(name = "Integrations", description = "Facade for automated booking agents (n8n)")
public class IntegrationController {
    private final SpecialtyService specialtyService;
    private final PersonalService personalService;
    private final ScheduleService scheduleService;
    private final PatientService patientService;
    private final AppointmentService appointmentService;

    @GetMapping("/specialties")
    @Operation(summary = "GET /api/integrations/n8n/specialties — list all available specialties")
    public ResponseEntity<List<SpecialtyResponse>> findSpecialties() {
        return ResponseEntity.ok(specialtyService.findAll());
    }

    @GetMapping("/doctors")
    @Operation(summary = "GET /api/integrations/n8n/doctors — list active doctors, filter by ?specialtyId={specialtyId}")
    public ResponseEntity<Page<PersonalResponse>> findDoctors(
            @RequestParam Long specialtyId,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(personalService.findAll(specialtyId, true, pageable));
    }

    @GetMapping("/schedules")
    @Operation(summary = "GET /api/integrations/n8n/schedules — list available slots for ?doctorId={doctorId}")
    public ResponseEntity<Page<ScheduleResponse>> findAvailableSchedules(
            @RequestParam Long doctorId,
            @PageableDefault(sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(scheduleService.findAll(doctorId, null, ScheduleStatus.AVAILABLE, LocalDateTime.now(), pageable));
    }

    @GetMapping("/patients/lookup")
    @Operation(summary = "GET /api/integrations/n8n/patients/lookup — find a registered patient by for ?phoneNumber={phoneNumber}")
    public ResponseEntity<PatientResponse> lookupPatient(@RequestParam String phoneNumber) {
        return ResponseEntity.ok(patientService.findByPhoneNumber(phoneNumber));
    }

    @PostMapping("/appointments")
    @Operation(summary = "POST /api/integrations/n8n/appointments — book a schedule slot for the patient identified by phoneNumber")
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.book(request, request.getClientId(), ERole.PATIENT.name()));
    }
}
