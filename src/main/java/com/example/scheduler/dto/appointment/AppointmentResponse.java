package com.example.scheduler.dto.appointment;

import com.example.scheduler.entity.enums.AppointmentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentResponse {
    private Long id;
    private Long scheduleId;
    private LocalDateTime scheduleStart;
    private LocalDateTime scheduleEnd;
    private String providerName;
    private String providerSpecialty;
    private String clientName;
    private String clientPhone;
    private String clientEmail;
    private AppointmentStatus status;
    private String notes;
    private LocalDateTime createdAt;
}
