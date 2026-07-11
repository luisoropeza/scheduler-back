package com.example.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PersonalRequest {
    @NotBlank
    private String name;
    private String email;
    private Long roleId;
    private Long specialtyId;
}
