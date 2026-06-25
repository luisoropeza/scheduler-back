package com.example.appointment.service;

import com.example.appointment.dto.AppointmentRequest;
import com.example.appointment.dto.AppointmentResponse;
import com.example.appointment.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppointmentService {
    AppointmentResponse book(AppointmentRequest request);
    AppointmentResponse findById(Long id);
    Page<AppointmentResponse> findByClientId(Long clientId, Pageable pageable);
    Page<AppointmentResponse> findByDoctorAndStatus(Long doctorId, AppointmentStatus status, Pageable pageable);
    AppointmentResponse confirm(Long id, Long doctorId);
    AppointmentResponse cancel(Long id, Long actorId);
    AppointmentResponse reschedule(Long id, Long newScheduleId, Long actorId);
}
