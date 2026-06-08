package com.example.scheduler.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppointmentRequest {

    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;

    @NotBlank(message = "Client name is required")
    private String clientName;

    @NotBlank(message = "Client phone is required")
    private String clientPhone;

    private String clientEmail;

    private String notes;
}
