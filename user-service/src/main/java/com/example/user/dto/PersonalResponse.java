package com.example.user.dto;

import lombok.Data;

@Data
public class PersonalResponse {
    private Long id;
    private String name;
    private String email;
    private boolean active;
    private String roleName;
    private String specialtyName;
}
