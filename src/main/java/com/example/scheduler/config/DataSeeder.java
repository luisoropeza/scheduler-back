package com.example.scheduler.config;

import com.example.scheduler.entity.Appointment;
import com.example.scheduler.entity.Provider;
import com.example.scheduler.entity.Schedule;
import com.example.scheduler.entity.enums.AppointmentStatus;
import com.example.scheduler.entity.enums.ScheduleStatus;
import com.example.scheduler.repository.AppointmentRepository;
import com.example.scheduler.repository.ProviderRepository;
import com.example.scheduler.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final ProviderRepository providerRepository;
    private final ScheduleRepository scheduleRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (providerRepository.count() > 0) {
            return;
        }

        Provider dr = providerRepository.save(Provider.builder()
                .name("Dr. Ana García")
                .specialty("General Medicine")
                .phone("+1-555-0101")
                .email("ana.garcia@clinic.com")
                .build());

        Provider dr2 = providerRepository.save(Provider.builder()
                .name("Dr. Carlos Méndez")
                .specialty("Dentistry")
                .phone("+1-555-0202")
                .email("carlos.mendez@clinic.com")
                .build());

        Provider dr3 = providerRepository.save(Provider.builder()
                .name("Dr. Laura Torres")
                .specialty("Pediatrics")
                .phone("+1-555-0303")
                .email("laura.torres@clinic.com")
                .build());

        LocalDateTime base = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        // Dr. García — 4 slots: 2 available, 1 booked (with confirmed appt), 1 booked (with pending appt)
        Schedule s1 = scheduleRepository.save(Schedule.builder()
                .provider(dr)
                .startTime(base.plusDays(1).withHour(9))
                .endTime(base.plusDays(1).withHour(10))
                .build());

        Schedule s2 = scheduleRepository.save(Schedule.builder()
                .provider(dr)
                .startTime(base.plusDays(1).withHour(10))
                .endTime(base.plusDays(1).withHour(11))
                .build());

        Schedule s3 = scheduleRepository.save(Schedule.builder()
                .provider(dr)
                .startTime(base.plusDays(2).withHour(9))
                .endTime(base.plusDays(2).withHour(10))
                .status(ScheduleStatus.BOOKED)
                .build());

        Schedule s4 = scheduleRepository.save(Schedule.builder()
                .provider(dr)
                .startTime(base.plusDays(2).withHour(11))
                .endTime(base.plusDays(2).withHour(12))
                .status(ScheduleStatus.BOOKED)
                .build());

        appointmentRepository.save(Appointment.builder()
                .schedule(s3)
                .clientName("John Doe")
                .clientPhone("+1-555-1001")
                .clientEmail("john.doe@email.com")
                .status(AppointmentStatus.CONFIRMED)
                .notes("Annual check-up")
                .build());

        appointmentRepository.save(Appointment.builder()
                .schedule(s4)
                .clientName("Maria Lopez")
                .clientPhone("+1-555-1002")
                .clientEmail("maria.lopez@email.com")
                .status(AppointmentStatus.PENDING)
                .build());

        // Dr. Méndez — 3 slots: 1 available, 1 booked (confirmed), 1 booked (cancelled)
        Schedule s5 = scheduleRepository.save(Schedule.builder()
                .provider(dr2)
                .startTime(base.plusDays(1).withHour(14))
                .endTime(base.plusDays(1).withHour(15))
                .build());

        Schedule s6 = scheduleRepository.save(Schedule.builder()
                .provider(dr2)
                .startTime(base.plusDays(3).withHour(10))
                .endTime(base.plusDays(3).withHour(11))
                .status(ScheduleStatus.BOOKED)
                .build());

        Schedule s7 = scheduleRepository.save(Schedule.builder()
                .provider(dr2)
                .startTime(base.plusDays(3).withHour(11))
                .endTime(base.plusDays(3).withHour(12))
                .status(ScheduleStatus.BOOKED)
                .build());

        appointmentRepository.save(Appointment.builder()
                .schedule(s6)
                .clientName("Peter Smith")
                .clientPhone("+1-555-2001")
                .clientEmail("peter.smith@email.com")
                .status(AppointmentStatus.CONFIRMED)
                .notes("Cleaning and x-ray")
                .build());

        appointmentRepository.save(Appointment.builder()
                .schedule(s7)
                .clientName("Susan Brown")
                .clientPhone("+1-555-2002")
                .status(AppointmentStatus.CANCELLED)
                .notes("Client rescheduled")
                .build());

        // Dr. Torres — 2 available slots
        scheduleRepository.save(Schedule.builder()
                .provider(dr3)
                .startTime(base.plusDays(2).withHour(15))
                .endTime(base.plusDays(2).withHour(16))
                .build());

        scheduleRepository.save(Schedule.builder()
                .provider(dr3)
                .startTime(base.plusDays(4).withHour(9))
                .endTime(base.plusDays(4).withHour(10))
                .build());
    }
}
