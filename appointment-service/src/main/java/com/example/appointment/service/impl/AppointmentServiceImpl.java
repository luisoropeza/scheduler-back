package com.example.appointment.service.impl;

import com.example.appointment.client.ScheduleClient;
import com.example.appointment.dto.AppointmentRequest;
import com.example.appointment.dto.AppointmentResponse;
import com.example.appointment.dto.ScheduleResponse;
import com.example.appointment.entity.Appointment;
import com.example.appointment.enums.AppointmentStatus;
import com.example.appointment.enums.ERole;
import com.example.appointment.enums.ScheduleStatus;
import com.example.appointment.exception.BusinessException;
import com.example.appointment.exception.ForbiddenException;
import com.example.appointment.exception.ResourceNotFoundException;
import com.example.appointment.mapper.AppointmentMapper;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final ScheduleClient scheduleClient;

    @Override
    @Transactional
    public AppointmentResponse book(AppointmentRequest request, Long userId, String role) {
        if (role.equals(ERole.PATIENT.name()) && !request.getClientId().equals(userId))
            throw new ForbiddenException("A patient can only book appointments for themselves");
        ScheduleResponse schedule = scheduleClient.findById(request.getScheduleId());
        if (!ScheduleStatus.AVAILABLE.name().equals(schedule.getStatus()))
            throw new BusinessException("This schedule slot is no longer available");
        if (schedule.getStartTime().isBefore(LocalDateTime.now()))
            throw new BusinessException("Cannot book a past schedule slot");
        scheduleClient.bookSchedule(request.getScheduleId());
        Appointment appointment = buildAppointment(schedule, request);
        if (role.equals(ERole.PATIENT.name())) {
            appointment.setStatus(AppointmentStatus.CONFIRMED);
        }
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    public AppointmentResponse findById(Long id) {
        return appointmentMapper.toResponse(getOrThrow(id));
    }

    @Override
    public Page<AppointmentResponse> findByClientId(Long clientId, Pageable pageable, Long userId, String role) {
        if (role.equals(ERole.PATIENT.name()))
            if(!clientId.equals(userId))
                throw new ForbiddenException("Not authorized to get those appointments");
        return appointmentRepository.findByClientId(clientId, pageable).map(appointmentMapper::toResponse);
    }

    @Override
    public Page<AppointmentResponse> findByDoctorAndStatus(Long doctorId, AppointmentStatus status, Pageable pageable, Long userId, String role) {
        if (role.equals(ERole.DOCTOR.name()))
            if(!doctorId.equals(userId))
                throw new ForbiddenException("Not authorized to get those appointments");
        return appointmentRepository.findAllByFilters(doctorId, status, pageable).map(appointmentMapper::toResponse);
    }

    @Override
    @Transactional
    public AppointmentResponse confirm(Long id, Long userId, String role) {
        Appointment appointment = getOrThrow(id);
        if (role.equals(ERole.DOCTOR.name()))
            if(!appointment.getDoctorId().equals(userId))
                throw new ForbiddenException("Not authorized to confirm this appointment");
        if (appointment.getStatus() != AppointmentStatus.PENDING)
            throw new BusinessException("Only pending appointments can be confirmed");
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment saved = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(Long id) {
        Appointment appointment = getOrThrow(id);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED)
            throw new BusinessException("Appointment is already cancelled");
        appointment.setStatus(AppointmentStatus.CANCELLED);
        scheduleClient.releaseSchedule(appointment.getScheduleId());
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse reschedule(Long id, Long newScheduleId) {
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
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
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
