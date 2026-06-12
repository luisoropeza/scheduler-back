package com.example.provider.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProviderRequest {
    @NotBlank
    private String name;
    private String specialty;
    private String phone;
    private String email;
}
