package com.example.schedule.config;

import com.example.schedule.entity.Schedule;
import com.example.schedule.entity.enums.ScheduleStatus;
import com.example.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Seeds schedule slots that mirror the provider-service seed (providers 1-3).
 * Assumes provider-service has already been seeded so IDs 1, 2, 3 are stable.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final ScheduleRepository scheduleRepository;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (scheduleRepository.count() > 0) return;

        LocalDateTime base = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        // Dr. Ana García (id=1) — 2 available, 2 booked
        save(1L, "Dr. Ana García", "General Medicine", base.plusDays(1).withHour(9),  base.plusDays(1).withHour(10),  ScheduleStatus.AVAILABLE);
        save(1L, "Dr. Ana García", "General Medicine", base.plusDays(1).withHour(10), base.plusDays(1).withHour(11),  ScheduleStatus.AVAILABLE);
        save(1L, "Dr. Ana García", "General Medicine", base.plusDays(2).withHour(9),  base.plusDays(2).withHour(10),  ScheduleStatus.BOOKED);
        save(1L, "Dr. Ana García", "General Medicine", base.plusDays(2).withHour(11), base.plusDays(2).withHour(12),  ScheduleStatus.BOOKED);

        // Dr. Carlos Méndez (id=2) — 1 available, 2 booked
        save(2L, "Dr. Carlos Méndez", "Dentistry", base.plusDays(1).withHour(14), base.plusDays(1).withHour(15), ScheduleStatus.AVAILABLE);
        save(2L, "Dr. Carlos Méndez", "Dentistry", base.plusDays(3).withHour(10), base.plusDays(3).withHour(11), ScheduleStatus.BOOKED);
        save(2L, "Dr. Carlos Méndez", "Dentistry", base.plusDays(3).withHour(11), base.plusDays(3).withHour(12), ScheduleStatus.BOOKED);

        // Dr. Laura Torres (id=3) — 2 available
        save(3L, "Dr. Laura Torres", "Pediatrics", base.plusDays(2).withHour(15), base.plusDays(2).withHour(16), ScheduleStatus.AVAILABLE);
        save(3L, "Dr. Laura Torres", "Pediatrics", base.plusDays(4).withHour(9),  base.plusDays(4).withHour(10), ScheduleStatus.AVAILABLE);
    }

    private void save(Long pid, String name, String specialty,
                      LocalDateTime start, LocalDateTime end, ScheduleStatus status) {
        scheduleRepository.save(Schedule.builder()
                .providerId(pid).providerName(name).providerSpecialty(specialty)
                .startTime(start).endTime(end).status(status).build());
    }
}
