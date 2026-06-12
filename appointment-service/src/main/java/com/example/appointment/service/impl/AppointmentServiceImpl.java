package com.example.appointment.service.impl;

import com.example.appointment.client.ScheduleClient;
import com.example.appointment.config.RabbitConfig;
import com.example.appointment.dto.AppointmentRequest;
import com.example.appointment.dto.AppointmentResponse;
import com.example.appointment.dto.ScheduleResponse;
import com.example.appointment.entity.Appointment;
import com.example.appointment.entity.enums.AppointmentStatus;
import com.example.appointment.event.AppointmentBookedEvent;
import com.example.appointment.exception.BusinessException;
import com.example.appointment.exception.ResourceNotFoundException;
import com.example.appointment.mapper.AppointmentMapper;
import com.example.appointment.repository.AppointmentRepository;
import com.example.appointment.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final ScheduleClient scheduleClient;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public AppointmentResponse book(AppointmentRequest request) {
        ScheduleResponse schedule = scheduleClient.findById(request.getScheduleId());

        if (!"AVAILABLE".equals(schedule.getStatus())) {
            throw new BusinessException("This schedule slot is no longer available");
        }
        if (schedule.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot book a schedule slot that has already passed");
        }

        scheduleClient.bookSchedule(request.getScheduleId());

        Appointment appointment = Appointment.builder()
                .scheduleId(schedule.getId())
                .providerId(schedule.getProviderId())
                .scheduleStart(schedule.getStartTime())
                .scheduleEnd(schedule.getEndTime())
                .providerName(schedule.getProviderName())
                .providerSpecialty(schedule.getProviderSpecialty())
                .clientName(request.getClientName())
                .clientPhone(request.getClientPhone())
                .clientEmail(request.getClientEmail())
                .notes(request.getNotes())
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE_NAME,
                RabbitConfig.BOOKING_ROUTING_KEY,
                new AppointmentBookedEvent(
                        saved.getId(), saved.getClientEmail(), saved.getClientName(),
                        saved.getProviderName(), saved.getProviderSpecialty(),
                        saved.getScheduleStart(), saved.getScheduleEnd(),
                        saved.getStatus().name(), saved.getNotes()));

        return appointmentMapper.toResponse(saved);
    }

    @Override
    public AppointmentResponse findById(Long id) {
        return appointmentMapper.toResponse(getOrThrow(id));
    }

    @Override
    public List<AppointmentResponse> findByClientPhone(String phone) {
        return appointmentMapper.toResponseList(
                appointmentRepository.findByClientPhoneOrderByCreatedAtDesc(phone));
    }

    @Override
    public List<AppointmentResponse> findByProviderAndStatus(Long providerId, AppointmentStatus status) {
        return appointmentMapper.toResponseList(
                appointmentRepository.findByProviderIdAndStatusOrderByScheduleStartAsc(providerId, status));
    }

    @Override
    @Transactional
    public AppointmentResponse confirm(Long id) {
        Appointment appointment = getOrThrow(id);
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BusinessException("Only pending appointments can be confirmed");
        }
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(Long id) {
        Appointment appointment = getOrThrow(id);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Appointment is already cancelled");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        scheduleClient.releaseSchedule(appointment.getScheduleId());
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    private Appointment getOrThrow(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
    }
}
