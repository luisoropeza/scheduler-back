package com.example.user.controller;

import com.example.user.dto.PatientResponse;
import com.example.user.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/patients")
@RequiredArgsConstructor
public class PatientInternalController {

    private final PatientService patientService;

    @GetMapping("/lookup")
    public ResponseEntity<PatientResponse> lookupByPhoneNumber(@RequestParam String phoneNumber) {
        return ResponseEntity.ok(patientService.findByPhoneNumber(phoneNumber));
    }
}
