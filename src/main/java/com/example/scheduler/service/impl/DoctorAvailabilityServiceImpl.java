package com.example.scheduler.service.impl;

import com.example.scheduler.dto.DoctorAvailability.AvailabilitySlotsResponse;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityRequest;
import com.example.scheduler.dto.DoctorAvailability.DoctorAvailabilityResponse;
import com.example.scheduler.entity.Appointment;
import com.example.scheduler.entity.DoctorAvailability;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.entity.ScheduleException;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.DoctorAvailabilityMapper;
import com.example.scheduler.repository.AppointmentRepository;
import com.example.scheduler.repository.DoctorAvailabilityRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.repository.ScheduleExceptionRepository;
import com.example.scheduler.service.DoctorAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorAvailabilityServiceImpl implements DoctorAvailabilityService {
    private final DoctorAvailabilityRepository availabilityRepository;
    private final ScheduleExceptionRepository scheduleExceptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PersonalRepository personalRepository;
    private final DoctorAvailabilityMapper mapper;

    @Override
    @Transactional
    public DoctorAvailabilityResponse addAvailability(Long doctorId, DoctorAvailabilityRequest request) {
        if (request.startTime().isAfter(request.endTime()) || request.startTime().equals(request.endTime()))
            throw new IllegalArgumentException("the start time must be after the end time");
        Personal doctor = personalRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));
        DoctorAvailability availability = DoctorAvailability.builder()
                .doctor(doctor)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .slotDurationMinutes(request.slotDurationMinutes())
                .active(true)
                .build();
        return mapper.toResponse(availabilityRepository.save(availability));
    }

    @Override
    public List<DoctorAvailabilityResponse> getDoctorAvailabilities(Long doctorId) {
        return mapper.toResponseList(availabilityRepository.findByDoctorIdAndActiveTrue(doctorId));
    }

    @Override
    public AvailabilitySlotsResponse getAvailableSlots(Long doctorId, LocalDate date) {
        boolean isFullDayBlocked = scheduleExceptionRepository.existsByDoctorIdAndDateAndIsFullDayBlockTrue(doctorId, date);
        if (isFullDayBlocked) {
            return new AvailabilitySlotsResponse(date, doctorId, List.of());
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<DoctorAvailability> availabilities = availabilityRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, dayOfWeek);

        if (availabilities.isEmpty()) {
            return new AvailabilitySlotsResponse(date, doctorId, List.of());
        }
        List<LocalTime> generatedSlots = createGeneratedSlots(availabilities);
        List<ScheduleException> partialExceptions = scheduleExceptionRepository.findByDoctorIdAndDate(doctorId, date)
                .stream()
                .filter(e -> !e.getIsFullDayBlock() && e.getStartTime() != null && e.getEndTime() != null)
                .toList();
        List<LocalTime> slotsAfterExceptions = generatedSlots.stream()
                .filter(slot -> partialExceptions.stream().noneMatch(e ->
                        !slot.isBefore(e.getStartTime()) && slot.isBefore(e.getEndTime())
                ))
                .toList();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        List<Appointment> existingAppointments = appointmentRepository.findByDoctorIdAndStartTimeBetweenAndStatusNot(
                doctorId,
                startOfDay,
                endOfDay,
                AppointmentStatus.CANCELLED
        );
        List<LocalTime> bookedStartTimes = existingAppointments.stream()
                .map(a -> a.getStartTime().toLocalTime())
                .toList();
        List<LocalTime> availableSlots = slotsAfterExceptions.stream()
                .filter(slot -> !bookedStartTimes.contains(slot))
                .sorted()
                .toList();
        return new AvailabilitySlotsResponse(date, doctorId, availableSlots);
    }

    private List<LocalTime> createGeneratedSlots(List<DoctorAvailability> availabilities) {
        List<LocalTime> generatedSlots = new ArrayList<>();
        for (DoctorAvailability availability : availabilities) {
            LocalTime current = availability.getStartTime();
            LocalTime end = availability.getEndTime();
            int duration = availability.getSlotDurationMinutes();

            while (current.plusMinutes(duration).isBefore(end) || current.plusMinutes(duration).equals(end)) {
                generatedSlots.add(current);
                current = current.plusMinutes(duration);
            }
        }
        return generatedSlots;
    }
}
