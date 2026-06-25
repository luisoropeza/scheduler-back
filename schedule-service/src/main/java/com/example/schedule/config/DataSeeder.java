package com.example.schedule.config;

import com.example.schedule.entity.Schedule;
import com.example.schedule.enums.ScheduleStatus;
import com.example.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component @RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {
    private final ScheduleRepository scheduleRepository;

    @Override @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (scheduleRepository.count() > 0) return;

        LocalDateTime base = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        save(1L, "Dr. Ana García", "General Medicine", "ana.garcia@clinic.com", base.plusDays(1).withHour(9),  base.plusDays(1).withHour(10),  ScheduleStatus.AVAILABLE);
        save(1L, "Dr. Ana García", "General Medicine", "ana.garcia@clinic.com", base.plusDays(1).withHour(10), base.plusDays(1).withHour(11),  ScheduleStatus.AVAILABLE);
        save(1L, "Dr. Ana García", "General Medicine", "ana.garcia@clinic.com", base.plusDays(2).withHour(9),  base.plusDays(2).withHour(10),  ScheduleStatus.BOOKED);
        save(2L, "Dr. Carlos Méndez", "Dentistry", "carlos.mendez@clinic.com", base.plusDays(1).withHour(14), base.plusDays(1).withHour(15), ScheduleStatus.AVAILABLE);
        save(2L, "Dr. Carlos Méndez", "Dentistry", "carlos.mendez@clinic.com", base.plusDays(3).withHour(10), base.plusDays(3).withHour(11), ScheduleStatus.BOOKED);
        save(3L, "Dr. Laura Torres", "Pediatrics", "laura.torres@clinic.com", base.plusDays(2).withHour(15), base.plusDays(2).withHour(16), ScheduleStatus.AVAILABLE);
        save(3L, "Dr. Laura Torres", "Pediatrics", "laura.torres@clinic.com", base.plusDays(4).withHour(9),  base.plusDays(4).withHour(10), ScheduleStatus.AVAILABLE);
    }

    private void save(Long doctorId, String name, String specialty, String email,
                      LocalDateTime start, LocalDateTime end, ScheduleStatus status) {
        scheduleRepository.save(Schedule.builder()
                .doctorId(doctorId).doctorName(name).doctorSpecialty(specialty).doctorEmail(email)
                .startTime(start).endTime(end).status(status).build());
    }
}
