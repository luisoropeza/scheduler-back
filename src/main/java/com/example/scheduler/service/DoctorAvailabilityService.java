package com.example.scheduler.service;

import com.example.scheduler.dto.DoctorAvailability.AvailabilitySlotsResponse;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityRequest;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityResponse;

import java.time.LocalDate;
import java.util.List;

public interface DoctorAvailabilityService {
    DoctorAvailabilityResponse addAvailability(Long doctorId, DoctorAvailabilityRequest request);
    List<DoctorAvailabilityResponse> getDoctorAvailabilities(Long doctorId);
    AvailabilitySlotsResponse getAvailableSlots(Long doctorId, LocalDate date);
}
