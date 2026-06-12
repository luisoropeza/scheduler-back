package com.example.provider.dto;

import lombok.Data;

@Data
public class ProviderResponse {
    private Long id;
    private String name;
    private String specialty;
    private String phone;
    private String email;
    private boolean active;
}
