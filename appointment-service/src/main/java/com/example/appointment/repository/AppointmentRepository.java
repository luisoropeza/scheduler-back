package com.example.appointment.repository;

import com.example.appointment.entity.Appointment;
import com.example.appointment.entity.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByClientPhoneOrderByCreatedAtDesc(String clientPhone);
    List<Appointment> findByProviderIdAndStatusOrderByScheduleStartAsc(Long providerId, AppointmentStatus status);
}
