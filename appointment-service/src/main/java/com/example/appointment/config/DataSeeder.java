package com.example.appointment.config;

import com.example.appointment.entity.Appointment;
import com.example.appointment.enums.AppointmentStatus;
import com.example.appointment.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component @RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {
    private final AppointmentRepository appointmentRepository;

    @Override @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (appointmentRepository.count() > 0) return;

        LocalDateTime base = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        // Slot 3 — Dr. Ana García, day+2 09:00
        appointmentRepository.save(Appointment.builder()
                .scheduleId(3L)
                .doctorId(1L)
                .scheduleStart(base.plusDays(2).withHour(9))
                .scheduleEnd(base.plusDays(2).withHour(10))
                .doctorName("Dr. Ana García")
                .doctorSpecialty("General Medicine")
                .doctorEmail("ana.garcia@clinic.com")
                .clientId(1L).clientName("John Smith")
                .clientEmail("john.smith@email.com")
                .status(AppointmentStatus.CONFIRMED).build());

        // Slot 4 — Dr. Carlos Méndez, day+3 10:00
        appointmentRepository.save(Appointment.builder()
                .scheduleId(4L)
                .doctorId(2L)
                .scheduleStart(base.plusDays(3).withHour(10))
                .scheduleEnd(base.plusDays(3).withHour(11))
                .doctorName("Dr. Carlos Méndez")
                .doctorSpecialty("Dentistry")
                .doctorEmail("carlos.mendez@clinic.com")
                .clientId(2L).clientName("María López").
                clientEmail("maria.lopez@email.com")
                .status(AppointmentStatus.PENDING).build());
    }
}
