package com.example.scheduler.dto.provider;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProviderRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String specialty;

    private String phone;

    private String email;
}
