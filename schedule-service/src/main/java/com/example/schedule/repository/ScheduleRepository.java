package com.example.schedule.repository;

import com.example.schedule.entity.Schedule;
import com.example.schedule.enums.ScheduleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("SELECT s FROM Schedule s WHERE " +
            "(:doctorId IS NULL OR s.doctorId = :doctorId) AND " +
            "(:status IS NULL OR s.status = :status) AND " +
            "(:after IS NULL OR s.startTime > :after) AND " +
            "(:specialty IS NULL OR LOWER(s.doctorSpecialty) = LOWER(:specialty))")
    Page<Schedule> findAllByFilters(
            @Param("doctorId") Long doctorId,
            @Param("specialty") String specialty,
            @Param("status") ScheduleStatus status,
            @Param("after") LocalDateTime after,
            Pageable pageable
    );
}
