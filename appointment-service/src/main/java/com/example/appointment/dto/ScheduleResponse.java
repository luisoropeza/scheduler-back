package com.example.appointment.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleResponse {
    private Long id;

    // Doctor
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialty;
    private String doctorEmail;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
}
