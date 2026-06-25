package com.example.user.dto;

import com.example.user.enums.ERole;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PersonalRequest {
    @NotBlank
    private String name;
    private String email;
    private ERole role;
    private Long specialtyId;
}
