package com.example.scheduler.service.impl;

import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.entity.Appointment;
import com.example.scheduler.entity.Schedule;
import com.example.scheduler.entity.enums.AppointmentStatus;
import com.example.scheduler.entity.enums.ScheduleStatus;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.AppointmentMapper;
import com.example.scheduler.repository.AppointmentRepository;
import com.example.scheduler.repository.ScheduleRepository;
import com.example.scheduler.service.AppointmentService;
import com.example.scheduler.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final AppointmentMapper appointmentMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public AppointmentResponse book(AppointmentRequest request) {
        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Schedule not found with id: " + request.getScheduleId()));

        if (schedule.getStatus() != ScheduleStatus.AVAILABLE) {
            throw new BusinessException("This schedule slot is no longer available");
        }
        if (schedule.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot book a schedule slot that has already passed");
        }

        schedule.setStatus(ScheduleStatus.BOOKED);
        scheduleRepository.save(schedule);

        Appointment appointment = Appointment.builder()
                .schedule(schedule)
                .clientName(request.getClientName())
                .clientPhone(request.getClientPhone())
                .clientEmail(request.getClientEmail())
                .notes(request.getNotes())
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        notificationService.sendAppointmentConfirmation(saved);
        return appointmentMapper.toResponse(saved);
    }

    @Override
    public AppointmentResponse findById(Long id) {
        return appointmentMapper.toResponse(getAppointmentOrThrow(id));
    }

    @Override
    public List<AppointmentResponse> findByClientPhone(String phone) {
        return appointmentMapper.toResponseList(
                appointmentRepository.findByClientPhoneOrderByCreatedAtDesc(phone));
    }

    @Override
    public List<AppointmentResponse> findByProviderAndStatus(Long providerId, AppointmentStatus status) {
        return appointmentMapper.toResponseList(
                appointmentRepository.findByScheduleProviderIdAndStatusOrderByScheduleStartTimeAsc(
                        providerId, status));
    }

    @Override
    @Transactional
    public AppointmentResponse confirm(Long id) {
        Appointment appointment = getAppointmentOrThrow(id);
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BusinessException("Only pending appointments can be confirmed");
        }
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(Long id) {
        Appointment appointment = getAppointmentOrThrow(id);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Appointment is already cancelled");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.getSchedule().setStatus(ScheduleStatus.AVAILABLE);
        scheduleRepository.save(appointment.getSchedule());
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    private Appointment getAppointmentOrThrow(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
    }
}
