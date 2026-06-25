package com.example.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppointmentRequest {
    @NotNull
    private Long scheduleId;
    @NotNull
    private Long clientId;
    @NotBlank
    private String clientName;
    private String clientEmail;
}
