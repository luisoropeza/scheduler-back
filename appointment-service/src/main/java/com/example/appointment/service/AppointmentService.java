package com.example.appointment.service;

import com.example.appointment.dto.AppointmentRequest;
import com.example.appointment.dto.AppointmentResponse;
import com.example.appointment.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppointmentService {
    AppointmentResponse book(AppointmentRequest request, Long userId, String role);
    AppointmentResponse findById(Long id);
    Page<AppointmentResponse> findByClientId(Long clientId, Pageable pageable, Long userId, String role);
    Page<AppointmentResponse> findByDoctorAndStatus(Long doctorId, AppointmentStatus status, Pageable pageable, Long userId, String role);
    AppointmentResponse confirm(Long id, Long userId, String role);
    AppointmentResponse cancel(Long id);
    AppointmentResponse reschedule(Long id, Long newScheduleId);
}
