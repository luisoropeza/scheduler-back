package com.example.scheduler.service.impl;

import com.example.scheduler.dto.appointment.AppointmentRequest;
import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.dto.appointment.AppointmentSummaryItem;
import com.example.scheduler.entity.Appointment;
import com.example.scheduler.entity.Patient;
import com.example.scheduler.entity.DoctorAvailability;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.AppointmentMapper;
import com.example.scheduler.repository.*;
import com.example.scheduler.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentServiceImpl implements AppointmentService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter CALENDAR_KEY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final PersonalRepository personalRepository;
    private final AppointmentMapper appointmentMapper;
    private final ScheduleExceptionRepository  scheduleExceptionRepository;
    private final DoctorAvailabilityRepository  doctorAvailabilityRepository;

    @Override
    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request) {
        LocalDate appointmentDate = request.startTime().toLocalDate();
        LocalTime requestedStartTime = request.startTime().toLocalTime();
        DayOfWeek dayOfWeek = appointmentDate.getDayOfWeek();
        boolean isBlocked = scheduleExceptionRepository.existsByDoctorIdAndDateAndIsFullDayBlockTrue(
                request.doctorId(), appointmentDate
        );
        if (isBlocked)
            throw new BusinessException("That schedule is blocked");
        List<DoctorAvailability> availabilities = doctorAvailabilityRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrue(request.doctorId(), dayOfWeek);
        boolean fitsInAvailability = availabilities.stream().anyMatch(a ->
                !requestedStartTime.isBefore(a.getStartTime()) &&
                        !request.endTime().toLocalTime().isAfter(a.getEndTime())
        );
        if (!fitsInAvailability)
            throw new BusinessException("That Schedule is out of journey");
        boolean isSlotTaken = appointmentRepository.existsOverlappingAppointment(
                request.doctorId(),
                request.startTime(),
                request.endTime(),
                List.of(AppointmentStatus.CANCELLED)
        );
        if (isSlotTaken)
            throw new BusinessException("That slot is already taken");
        Personal doctor = personalRepository.getReferenceById(request.doctorId());
        Patient patient = patientRepository.getReferenceById(request.patientId());
        Appointment appointment = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(AppointmentStatus.PENDING)
                .build();

        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    public AppointmentResponse findAppointmentById(Long appointmentId, Long userId, String role) {
        var appointment = getAppointmentOrThrowById(appointmentId);
        verifyPermission(appointment, userId, role);
        return appointmentMapper.toResponse(appointment);
    }

    @Override
    public Page<AppointmentResponse> findAllAppointments(Long doctorId, Long patientId, AppointmentStatus status, Pageable pageable) {
        return appointmentRepository.findAllByFilters(doctorId, patientId, status, pageable).map(appointmentMapper::toResponse);
    }

    @Override
    @Transactional
    public AppointmentResponse confirmAppointmentById(Long AppointmentId, Long userId, String role) {
        var appointment = getAppointmentOrThrowById(AppointmentId);
        verifyPermission(appointment, userId, role);
        if (appointment.getStatus() != AppointmentStatus.PENDING)
            throw new BusinessException("Just can confirm an appointment pending");
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointmentById(Long AppointmentId, Long userId, String role) {
        var appointment = getAppointmentOrThrowById(AppointmentId);
        verifyPermission(appointment, userId, role);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED)
            throw new BusinessException("This appointment is already cancelled");
        appointment.setStatus(AppointmentStatus.CANCELLED);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    public Map<AppointmentStatus, List<AppointmentSummaryItem>> getBoardByRange(LocalDate from, LocalDate to, Long doctorId, Long patientId) {
        return groupByStatus(appointmentRepository.findByFiltersAndDateRange(doctorId, patientId, from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
    }

    @Override
    public Map<String, List<AppointmentSummaryItem>> getCalendar(int month, int year, Long doctorId, Long patientId) {
        var monthStart = LocalDate.of(year, month, 1);
        return groupByDay(appointmentRepository.findByFiltersAndDateRange(doctorId, patientId, monthStart.atStartOfDay(), monthStart.plusMonths(1).atStartOfDay()));
    }

    private Map<AppointmentStatus, List<AppointmentSummaryItem>> groupByStatus(List<Appointment> appointments) {
        Map<AppointmentStatus, List<AppointmentSummaryItem>> board = new EnumMap<>(AppointmentStatus.class);
        for (AppointmentStatus status : AppointmentStatus.values())
            board.put(status, new ArrayList<>());
        for (Appointment appointment : appointments)
            board.get(appointment.getStatus()).add(toSummaryItem(appointment));
        return board;
    }

    private Map<String, List<AppointmentSummaryItem>> groupByDay(List<Appointment> appointments) {
        Map<String, List<AppointmentSummaryItem>> calendar = new LinkedHashMap<>();
        for (Appointment appointment : appointments) {
            String key = appointment.getStartTime().toLocalDate().format(CALENDAR_KEY_FORMATTER);
            calendar.computeIfAbsent(key, _ -> new ArrayList<>()).add(toSummaryItem(appointment));
        }
        return calendar;
    }

    private AppointmentSummaryItem toSummaryItem(Appointment appointment) {
        var startTime = appointment.getStartTime();
        return new AppointmentSummaryItem(
                appointment.getPatient().getAccount().getName(),
                appointment.getDoctor().getAccount().getName(),
                startTime.toLocalDate(),
                startTime.format(TIME_FORMATTER));
    }

    private Appointment getAppointmentOrThrowById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));
    }

    private void verifyPermission(Appointment appointment, Long userId, String role) {
        if(role.equals(ERole.DOCTOR.name()))
            if (!appointment.getDoctor().getId().equals(userId))
                throw new ForbiddenException("Not authorize to do this");
        if(role.equals(ERole.PATIENT.name()))
            if (!appointment.getPatient().getId().equals(userId))
                throw new ForbiddenException("Not authorize to do this");
    }
}
