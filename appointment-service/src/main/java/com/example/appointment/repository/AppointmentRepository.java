package com.example.appointment.repository;

import com.example.appointment.entity.Appointment;
import com.example.appointment.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @Query("SELECT a FROM Appointment a WHERE " +
            "(:doctorId IS NULL OR a.doctorId = :doctorId) AND " +
            "(:status IS NULL OR a.status = :status)")
    Page<Appointment> findAllByFilters(@Param("doctorId") Long doctorId, @Param("status") AppointmentStatus status, Pageable pageable);
    Page<Appointment> findByClientId(Long clientId, Pageable pageable);
}
