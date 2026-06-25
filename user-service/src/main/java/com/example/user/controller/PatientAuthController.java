package com.example.user.controller;

import com.example.user.dto.LoginRequest;
import com.example.user.dto.LoginResponse;
import com.example.user.dto.PatientRegisterRequest;
import com.example.user.service.PatientAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/patient")
@RequiredArgsConstructor
@Tag(name = "Patient Auth", description = "Patient authentication")
public class PatientAuthController {
    private final PatientAuthService authService;

    @PostMapping("/register")
    @Operation(summary = "POST /api/auth/patient/register — register a new patient and return a JWT token")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody PatientRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "POST /api/auth/patient/login — authenticate a patient and return a JWT token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
