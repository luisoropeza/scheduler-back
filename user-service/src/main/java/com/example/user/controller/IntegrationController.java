package com.example.user.controller;

import com.example.user.dto.PatientResponse;
import com.example.user.dto.PersonalResponse;
import com.example.user.dto.SpecialtyResponse;
import com.example.user.service.PatientService;
import com.example.user.service.PersonalService;
import com.example.user.service.SpecialtyService;
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

import java.util.List;

/**
 * Read-only browsing facade for automated callers (currently the n8n WhatsApp workflow).
 * Authenticated via a static API key ({@link com.example.user.security.ApiKeyAuthFilter}),
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
    private final PatientService patientService;

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

    @GetMapping("/patients/lookup")
    @Operation(summary = "GET /api/integrations/n8n/patients/lookup — find a registered patient by ?phoneNumber={phoneNumber}")
    public ResponseEntity<PatientResponse> lookupPatient(@RequestParam String phoneNumber) {
        return ResponseEntity.ok(patientService.findByPhoneNumber(phoneNumber));
    }
}
