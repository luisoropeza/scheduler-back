package com.example.appointment.event;

import java.time.LocalDateTime;

public record AppointmentBookedEvent(
        Long appointmentId,
        String eventType,
        String actorType,
        String clientEmail,
        String clientName,
        String doctorEmail,
        String doctorName,
        String doctorSpecialty,
        LocalDateTime scheduleStart,
        LocalDateTime scheduleEnd,
        String status
) {}
