package com.example.user.controller;

import com.example.user.dto.SpecialtyResponse;
import com.example.user.service.SpecialtyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/specialties")
@RequiredArgsConstructor
@Tag(name = "Specialties", description = "Available specialties")
public class SpecialtyController {
    private final SpecialtyService specialtyService;

    @GetMapping
    @Operation(summary = "GET /api/specialties — list all available specialties")
    public ResponseEntity<List<SpecialtyResponse>> findAll() {
        return ResponseEntity.ok(specialtyService.findAll());
    }
}
