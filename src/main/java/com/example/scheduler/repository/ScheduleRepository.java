package com.example.scheduler.repository;

import com.example.scheduler.entity.Schedule;
import com.example.scheduler.entity.enums.ScheduleStatus;
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
