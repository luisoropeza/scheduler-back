package com.example.user.controller;

import com.example.user.dto.LoginRequest;
import com.example.user.dto.LoginResponse;
import com.example.user.dto.PatientRegisterRequest;
import com.example.user.dto.PersonalRegisterRequest;
import com.example.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Patient and staff authentication")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/patient/register")
    @Operation(summary = "POST /api/auth/patient/register — register a new patient and return a JWT token")
    public ResponseEntity<LoginResponse> registerPatient(@Valid @RequestBody PatientRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerPatient(request));
    }

    @PostMapping("/patient/login")
    @Operation(summary = "POST /api/auth/patient/login — authenticate a patient and return a JWT token")
    public ResponseEntity<LoginResponse> loginPatient(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginPatient(request));
    }

    @PostMapping("/personal/register")
    @Operation(summary = "POST /api/auth/personal/register — register a new staff member and return a JWT token")
    public ResponseEntity<LoginResponse> registerPersonal(@Valid @RequestBody PersonalRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerPersonal(request));
    }

    @PostMapping("/personal/login")
    @Operation(summary = "POST /api/auth/personal/login — authenticate a staff member and return a JWT token")
    public ResponseEntity<LoginResponse> loginPersonal(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginPersonal(request));
    }
}
