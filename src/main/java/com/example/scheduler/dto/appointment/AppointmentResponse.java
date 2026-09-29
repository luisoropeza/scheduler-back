package com.example.scheduler.dto.appointment;

import lombok.Data;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,

        // Schedule=
        LocalDateTime startTime,
        LocalDateTime endTime,

        // Doctor
        Long doctorId,
        String doctorName,
        String doctorSpecialty,
        String doctorEmail,

        // Client
        Long patientId,
        String patientName,
        String patientEmail,

        String status,
        LocalDateTime createdAt
) {}
