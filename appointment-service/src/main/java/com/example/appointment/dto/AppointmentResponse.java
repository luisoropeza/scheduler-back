package com.example.appointment.dto;

import com.example.appointment.enums.AppointmentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentResponse {
    private Long id;

    // Schedule
    private Long scheduleId;
    private LocalDateTime scheduleStart;
    private LocalDateTime scheduleEnd;

    // Doctor
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialty;
    private String doctorEmail;

    // Client
    private Long clientId;
    private String clientName;
    private String clientEmail;

    private AppointmentStatus status;
    private LocalDateTime createdAt;
}
