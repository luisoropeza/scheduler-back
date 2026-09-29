package com.example.scheduler.dto.appointment;

import java.time.LocalDate;

public record AppointmentSummaryItem(
        String clientName,
        String doctorName,
        LocalDate appointmentDate,
        String appointmentTime
) {}
