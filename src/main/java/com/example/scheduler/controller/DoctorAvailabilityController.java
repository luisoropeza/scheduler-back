package com.example.scheduler.controller;

import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityRequest;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityResponse;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilitySlotsResponse;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.security.SecurityUtils;
import com.example.scheduler.service.DoctorAvailabilityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctorAvailability")
@RequiredArgsConstructor
@Tag(name = "Doctor Availabilities", description = "Doctor Availability Controller")
public class DoctorAvailabilityController {
    private final DoctorAvailabilityService doctorAvailabilityService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR')")
    public ResponseEntity<DoctorAvailabilityResponse> addDoctorAvailability(@RequestBody DoctorAvailabilityRequest request, Authentication auth){
        return ResponseEntity.ok(doctorAvailabilityService.addDoctorAvailability(Long.parseLong(auth.getName()), request));
    }

    @GetMapping("/{doctorId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ASSISTANT', 'PATIENT')")
    public ResponseEntity<List<DoctorAvailabilityResponse>> getAllDoctorAvailability(@PathVariable Long doctorId, Authentication auth){
        var role = SecurityUtils.extractRole(auth);
        if(role.equals(ERole.DOCTOR.name()))
            return ResponseEntity.ok(doctorAvailabilityService.getDoctorAvailabilities(Long.parseLong(auth.getName())));
        return ResponseEntity.ok(doctorAvailabilityService.getDoctorAvailabilities(doctorId));
    }

    @GetMapping("/{doctorId}/availables")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ASSISTANT', 'PATIENT')")
    public ResponseEntity<DoctorAvailabilitySlotsResponse> getDoctorAvailabilitySlots(@PathVariable Long doctorId, @RequestParam LocalDate date, Authentication auth){
        var role = SecurityUtils.extractRole(auth);
        if (role.equals(ERole.DOCTOR.name()))
            return  ResponseEntity.ok(doctorAvailabilityService.getDoctorAvailableSlots(Long.parseLong(auth.getName()), date));
        return ResponseEntity.ok(doctorAvailabilityService.getDoctorAvailableSlots(doctorId, date));
    }
}
