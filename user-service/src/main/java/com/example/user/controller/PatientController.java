package com.example.user.controller;

import com.example.user.dto.PatientRequest;
import com.example.user.dto.PatientResponse;
import com.example.user.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Manage patients")
public class PatientController {
    private final PatientService patientService;

    @GetMapping
    @Operation(summary = "GET /api/patients — list all active patients")
    public ResponseEntity<Page<PatientResponse>> findAll(
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(patientService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "GET /api/patients/{id} — get a patient by ID")
    public ResponseEntity<PatientResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "PUT /api/patients/{id} — update patient information")
    public ResponseEntity<PatientResponse> update(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return ResponseEntity.ok(patientService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "DELETE /api/patients/{id} — deactivate (soft-delete) a patient account")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        patientService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
