package com.example.appointment.config;

import com.example.appointment.entity.Appointment;
import com.example.appointment.entity.enums.AppointmentStatus;
import com.example.appointment.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Seeds sample appointments.
 * scheduleId values must match the schedule-service seed order (slots 3, 4, 6, 7 are BOOKED).
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (appointmentRepository.count() > 0) return;

        LocalDateTime base = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        // Slot 3 — Dr. Ana García, day+2 09:00
        appointmentRepository.save(Appointment.builder()
                .scheduleId(3L).providerId(1L)
                .scheduleStart(base.plusDays(2).withHour(9)).scheduleEnd(base.plusDays(2).withHour(10))
                .providerName("Dr. Ana García").providerSpecialty("General Medicine")
                .clientName("John Doe").clientPhone("+1-555-1001").clientEmail("john.doe@email.com")
                .status(AppointmentStatus.CONFIRMED).notes("Annual check-up").build());

        // Slot 4 — Dr. Ana García, day+2 11:00
        appointmentRepository.save(Appointment.builder()
                .scheduleId(4L).providerId(1L)
                .scheduleStart(base.plusDays(2).withHour(11)).scheduleEnd(base.plusDays(2).withHour(12))
                .providerName("Dr. Ana García").providerSpecialty("General Medicine")
                .clientName("Maria Lopez").clientPhone("+1-555-1002").clientEmail("maria.lopez@email.com")
                .status(AppointmentStatus.PENDING).build());

        // Slot 6 — Dr. Carlos Méndez, day+3 10:00
        appointmentRepository.save(Appointment.builder()
                .scheduleId(6L).providerId(2L)
                .scheduleStart(base.plusDays(3).withHour(10)).scheduleEnd(base.plusDays(3).withHour(11))
                .providerName("Dr. Carlos Méndez").providerSpecialty("Dentistry")
                .clientName("Peter Smith").clientPhone("+1-555-2001").clientEmail("peter.smith@email.com")
                .status(AppointmentStatus.CONFIRMED).notes("Cleaning and x-ray").build());

        // Slot 7 — Dr. Carlos Méndez, day+3 11:00
        appointmentRepository.save(Appointment.builder()
                .scheduleId(7L).providerId(2L)
                .scheduleStart(base.plusDays(3).withHour(11)).scheduleEnd(base.plusDays(3).withHour(12))
                .providerName("Dr. Carlos Méndez").providerSpecialty("Dentistry")
                .clientName("Susan Brown").clientPhone("+1-555-2002")
                .status(AppointmentStatus.CANCELLED).notes("Client rescheduled").build());
    }
}
