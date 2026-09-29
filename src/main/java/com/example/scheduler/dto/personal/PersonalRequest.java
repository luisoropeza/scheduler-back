package com.example.scheduler.dto.personal;

import jakarta.validation.constraints.NotBlank;

public record PersonalRequest(
        @NotBlank
        String name,
        String email,
        Long specialtyId
) {}
