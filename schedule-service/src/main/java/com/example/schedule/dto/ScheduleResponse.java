package com.example.schedule.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleResponse {
    private Long id;
    private Long providerId;
    private String providerName;
    private String providerSpecialty;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
}
