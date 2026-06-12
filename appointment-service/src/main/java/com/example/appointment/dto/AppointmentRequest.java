package com.example.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppointmentRequest {
    @NotNull
    private Long scheduleId;
    @NotBlank
    private String clientName;
    @NotBlank
    private String clientPhone;
    private String clientEmail;
    private String notes;
}
