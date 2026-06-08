package com.example.scheduler.controller;

import com.example.scheduler.dto.provider.ProviderRequest;
import com.example.scheduler.dto.provider.ProviderResponse;
import com.example.scheduler.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
@Tag(name = "Providers", description = "Manage service providers (doctors, specialists, etc.)")
public class ProviderController {

    private final ProviderService providerService;

    @GetMapping
    @Operation(summary = "List all active providers, optionally filtered by specialty")
    public ResponseEntity<List<ProviderResponse>> findAll(
            @RequestParam(required = false) String specialty) {
        return ResponseEntity.ok(providerService.findAll(specialty));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a provider by ID")
    public ResponseEntity<ProviderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(providerService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Register a new provider")
    public ResponseEntity<ProviderResponse> create(@Valid @RequestBody ProviderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(providerService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update provider information")
    public ResponseEntity<ProviderResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProviderRequest request) {
        return ResponseEntity.ok(providerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a provider (soft delete)")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        providerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
