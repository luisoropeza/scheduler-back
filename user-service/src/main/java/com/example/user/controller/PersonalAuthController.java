package com.example.user.controller;

import com.example.user.dto.LoginRequest;
import com.example.user.dto.LoginResponse;
import com.example.user.dto.PersonalRegisterRequest;
import com.example.user.service.PersonalAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/personal")
@RequiredArgsConstructor
@Tag(name = "Personal Auth", description = "Staff authentication")
public class PersonalAuthController {
    private final PersonalAuthService authService;

    @PostMapping("/register")
    @Operation(summary = "POST /api/auth/personal/register — register a new staff member and return a JWT token")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody PersonalRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "POST /api/auth/personal/login — authenticate a staff member and return a JWT token")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
