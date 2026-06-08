package com.example.scheduler.repository;

import com.example.scheduler.entity.Appointment;
import com.example.scheduler.entity.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByClientPhoneOrderByCreatedAtDesc(String clientPhone);

    List<Appointment> findByScheduleProviderIdAndStatusOrderByScheduleStartTimeAsc(
            Long providerId, AppointmentStatus status);
}
