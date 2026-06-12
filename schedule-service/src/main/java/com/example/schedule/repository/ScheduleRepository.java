package com.example.schedule.repository;

import com.example.schedule.entity.Schedule;
import com.example.schedule.entity.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByProviderId(Long providerId);
    List<Schedule> findByProviderIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
            Long providerId, ScheduleStatus status, LocalDateTime after);
}
