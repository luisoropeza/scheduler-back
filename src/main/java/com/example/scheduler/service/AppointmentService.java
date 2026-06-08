package com.example.scheduler.service;

import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.entity.enums.AppointmentStatus;

import java.util.List;

public interface AppointmentService {

    AppointmentResponse book(AppointmentRequest request);

    AppointmentResponse findById(Long id);

    List<AppointmentResponse> findByClientPhone(String phone);

    List<AppointmentResponse> findByProviderAndStatus(Long providerId, AppointmentStatus status);

    AppointmentResponse confirm(Long id);

    AppointmentResponse cancel(Long id);
}
