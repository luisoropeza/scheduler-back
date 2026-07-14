package com.example.appointment.controller;

import com.example.appointment.client.PatientClient;
import com.example.appointment.dto.AppointmentRequest;
import com.example.appointment.dto.AppointmentResponse;
import com.example.appointment.dto.IntegrationBookingRequest;
import com.example.appointment.dto.PatientResponse;
import com.example.appointment.enums.ERole;
import com.example.appointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Booking facade for automated callers (currently the n8n WhatsApp workflow).
 * Authenticated via a static API key ({@link com.example.appointment.security.ApiKeyAuthFilter}),
 * not a per-patient JWT, since the caller only knows the patient's phone number.
 */
@RestController
@RequestMapping("/api/integrations/n8n")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTEGRATION')")
@Tag(name = "Integrations", description = "Facade for automated booking agents (n8n)")
public class IntegrationController {
    private final PatientClient patientClient;
    private final AppointmentService appointmentService;

    @PostMapping("/appointments")
    @Operation(summary = "POST /api/integrations/n8n/appointments — book a schedule slot for the patient identified by phoneNumber")
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody IntegrationBookingRequest request) {
        PatientResponse patient = patientClient.findByPhoneNumber(request.getPhoneNumber());
        AppointmentRequest appointmentRequest = new AppointmentRequest();
        appointmentRequest.setScheduleId(request.getScheduleId());
        appointmentRequest.setClientId(patient.getId());
        appointmentRequest.setClientName(patient.getName());
        appointmentRequest.setClientEmail(patient.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.book(appointmentRequest, patient.getId(), ERole.PATIENT.name()));
    }
}
