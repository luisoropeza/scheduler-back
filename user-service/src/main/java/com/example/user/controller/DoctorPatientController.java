package com.example.user.controller;

import com.example.user.dto.PatientResponse;
import com.example.user.dto.PersonalResponse;
import com.example.user.service.PatientService;
import com.example.user.service.PersonalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.user.security.SecurityUtils;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Doctor-Patient", description = "Manage doctor-patient relationships")
public class DoctorPatientController {
    private final PersonalService personalService;
    private final PatientService patientService;

    @PostMapping("/api/personal/{doctorId}/patients/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST')")
    @Operation(summary = "POST /api/personal/{doctorId}/patients/{patientId} — assign a patient to a doctor")
    public ResponseEntity<Void> assign(
            @PathVariable Long doctorId,
            @PathVariable Long patientId,
            Authentication auth
    ) {
        personalService.assignPatient(doctorId, patientId, Long.parseLong(auth.getName()), SecurityUtils.extractRole(auth));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/personal/{doctorId}/patients/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST')")
    @Operation(summary = "DELETE /api/personal/{doctorId}/patients/{patientId} — remove a patient from a doctor")
    public ResponseEntity<Void> remove(
            @PathVariable Long doctorId,
            @PathVariable Long patientId,
            Authentication auth
    ) {
        personalService.removePatient(doctorId, patientId, Long.parseLong(auth.getName()), SecurityUtils.extractRole(auth));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/personal/{doctorId}/patients")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST')")
    @Operation(summary = "GET /api/personal/{doctorId}/patients — list all patients assigned to a doctor")
    public ResponseEntity<List<PatientResponse>> getPatients(@PathVariable Long doctorId) {
        return ResponseEntity.ok(personalService.getPatientsOfDoctor(doctorId));
    }

    @GetMapping("/api/patients/{patientId}/doctors")
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST')")
    @Operation(summary = "GET /api/patients/{patientId}/doctors — list all doctors assigned to a patient")
    public ResponseEntity<List<PersonalResponse>> getDoctors(@PathVariable Long patientId) {
        return ResponseEntity.ok(patientService.getDoctorsOfPatient(patientId));
    }
}
