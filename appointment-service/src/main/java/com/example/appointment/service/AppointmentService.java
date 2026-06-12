package com.example.appointment.service;

import com.example.appointment.dto.AppointmentRequest;
import com.example.appointment.dto.AppointmentResponse;
import com.example.appointment.entity.enums.AppointmentStatus;

import java.util.List;

public interface AppointmentService {
    AppointmentResponse book(AppointmentRequest request);
    AppointmentResponse findById(Long id);
    List<AppointmentResponse> findByClientPhone(String phone);
    List<AppointmentResponse> findByProviderAndStatus(Long providerId, AppointmentStatus status);
    AppointmentResponse confirm(Long id);
    AppointmentResponse cancel(Long id);
}
