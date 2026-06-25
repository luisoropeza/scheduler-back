package com.example.appointment.service.impl;

import com.example.appointment.client.ScheduleClient;
import com.example.appointment.config.RabbitConfig;
import com.example.appointment.dto.AppointmentRequest;
import com.example.appointment.dto.AppointmentResponse;
import com.example.appointment.dto.ScheduleResponse;
import com.example.appointment.entity.Appointment;
import com.example.appointment.enums.AppointmentStatus;
import com.example.appointment.enums.ScheduleStatus;
import com.example.appointment.event.AppointmentBookedEvent;
import com.example.appointment.exception.BusinessException;
import com.example.appointment.exception.ResourceNotFoundException;
import com.example.appointment.mapper.AppointmentMapper;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final ScheduleClient scheduleClient;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public AppointmentResponse book(AppointmentRequest request) {
        ScheduleResponse schedule = scheduleClient.findById(request.getScheduleId());
        if (!ScheduleStatus.AVAILABLE.name().equals(schedule.getStatus()))
            throw new BusinessException("This schedule slot is no longer available");
        if (schedule.getStartTime().isBefore(LocalDateTime.now()))
            throw new BusinessException("Cannot book a past schedule slot");
        scheduleClient.bookSchedule(request.getScheduleId());
        Appointment appointment = buildAppointment(schedule, request);
        Appointment saved = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(saved);
    }

    public AppointmentResponse findById(Long id) {
        return appointmentMapper.toResponse(getOrThrow(id));
    }

    public Page<AppointmentResponse> findByClientId(Long clientId, Pageable pageable) {
        return appointmentRepository.findByClientId(clientId, pageable).map(appointmentMapper::toResponse);
    }

    public Page<AppointmentResponse> findByDoctorAndStatus(Long personalId, AppointmentStatus status, Pageable pageable) {
        return appointmentRepository.findAllByFilters(personalId, status, pageable).map(appointmentMapper::toResponse);
    }

    @Transactional
    public AppointmentResponse confirm(Long id, Long doctorId) {
        Appointment appointment = getOrThrow(id);
        if (!appointment.getDoctorId().equals(doctorId))
            throw new BusinessException("Not authorized to confirm this appointment");
        if (appointment.getStatus() != AppointmentStatus.PENDING)
            throw new BusinessException("Only pending appointments can be confirmed");
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment saved = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(appointmentRepository.save(saved));
    }

    @Transactional
    public AppointmentResponse cancel(Long id, Long actorId) {
        Appointment appointment = getOrThrow(id);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED)
            throw new BusinessException("Appointment is already cancelled");
        appointment.setStatus(AppointmentStatus.CANCELLED);
        scheduleClient.releaseSchedule(appointment.getScheduleId());
        Appointment saved = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(saved);
    }

    @Transactional
    public AppointmentResponse reschedule(Long id, Long newScheduleId, Long actorId) {
        Appointment appointment = getOrThrow(id);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED)
            throw new BusinessException("Cannot reschedule a cancelled appointment");
        ScheduleResponse newSchedule = scheduleClient.findById(newScheduleId);
        if (!ScheduleStatus.AVAILABLE.name().equals(newSchedule.getStatus()))
            throw new BusinessException("New schedule slot is not available");
        if (newSchedule.getStartTime().isBefore(LocalDateTime.now()))
            throw new BusinessException("Cannot reschedule to a past slot");
        scheduleClient.bookSchedule(newSchedule.getId());
        scheduleClient.releaseSchedule(appointment.getScheduleId());
        appointment.setScheduleId(newSchedule.getId());
        appointment.setScheduleStart(newSchedule.getStartTime());
        appointment.setScheduleEnd(newSchedule.getEndTime());
        appointment.setStatus(AppointmentStatus.PENDING);
        Appointment saved = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(saved);
    }

    private void publishEvent(Appointment a, String eventType, String actorType) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.BOOKING_ROUTING_KEY,
                new AppointmentBookedEvent(a.getId(), eventType, actorType,
                        a.getClientEmail(), a.getClientName(),
                        a.getDoctorEmail(), a.getDoctorName(), a.getDoctorSpecialty(),
                        a.getScheduleStart(), a.getScheduleEnd(), a.getStatus().name()));
    }

    private Appointment getOrThrow(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
    }

    private Appointment buildAppointment(ScheduleResponse schedule, AppointmentRequest request) {
        return Appointment.builder()
                .scheduleId(schedule.getId())
                .doctorId(schedule.getDoctorId())
                .scheduleStart(schedule.getStartTime())
                .scheduleEnd(schedule.getEndTime())
                .doctorName(schedule.getDoctorName())
                .doctorSpecialty(schedule.getDoctorSpecialty())
                .doctorEmail(schedule.getDoctorEmail())
                .clientId(request.getClientId())
                .clientName(request.getClientName())
                .clientEmail(request.getClientEmail())
                .build();
    }
}
