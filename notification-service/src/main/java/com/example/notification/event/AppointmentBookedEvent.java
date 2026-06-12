package com.example.notification.event;

import java.time.LocalDateTime;

public record AppointmentBookedEvent(
        Long appointmentId,
        String clientEmail,
        String clientName,
        String providerName,
        String providerSpecialty,
        LocalDateTime scheduleStart,
        LocalDateTime scheduleEnd,
        String status,
        String notes
) {}
