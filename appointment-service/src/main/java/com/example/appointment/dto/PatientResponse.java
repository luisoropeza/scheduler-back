package com.example.appointment.dto;

import lombok.Data;

@Data
public class PatientResponse {
    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private boolean active;
}
