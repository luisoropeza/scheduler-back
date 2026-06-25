package com.example.schedule.service.impl;

import com.example.schedule.client.PersonalClient;
import com.example.schedule.dto.PersonalResponse;
import com.example.schedule.dto.ScheduleRequest;
import com.example.schedule.dto.ScheduleResponse;
import com.example.schedule.entity.Schedule;
import com.example.schedule.enums.ScheduleStatus;
import com.example.schedule.exception.BusinessException;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.mapper.ScheduleMapper;
import com.example.schedule.repository.ScheduleRepository;
import com.example.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleServiceImpl implements ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final PersonalClient personalClient;
    private final ScheduleMapper scheduleMapper;

    public Page<ScheduleResponse> findAll(
            Long doctorId,
            String specialty,
            ScheduleStatus status,
            LocalDateTime after,
            Pageable pageable) {
        if(doctorId != null)
            getActiveDoctorOrThrow(doctorId);
        return scheduleRepository
                .findAllByFilters(
                        doctorId,
                        specialty, status == null? ScheduleStatus.AVAILABLE : status,
                        after == null? LocalDateTime.now() : after,
                        pageable)
                .map(scheduleMapper::toResponse);
    }

    public ScheduleResponse getById(Long id) {
        return scheduleMapper.toResponse(getScheduleOrThrow(id));
    }

    @Transactional
    public ScheduleResponse create(Long doctorId, ScheduleRequest request) {
        PersonalResponse doctor = getActiveDoctorOrThrow(doctorId);
        validateSlotTimes(request);
        return scheduleMapper.toResponse(scheduleRepository.save(buildSchedule(doctor, request)));
    }

    @Transactional
    public List<ScheduleResponse> createBatch(Long doctorId, List<ScheduleRequest> requests) {
        PersonalResponse doctor = getActiveDoctorOrThrow(doctorId);
        requests.forEach(this::validateSlotTimes);
        return scheduleMapper.toResponseList(scheduleRepository.saveAll(requests.stream().map(r -> buildSchedule(doctor, r)).toList()));
    }

    @Transactional
    public ScheduleResponse book(Long id) {
        Schedule schedule = getScheduleOrThrow(id);
        if (schedule.getStatus() != ScheduleStatus.AVAILABLE) throw new BusinessException("Schedule slot is not available");
        schedule.setStatus(ScheduleStatus.BOOKED);
        return scheduleMapper.toResponse(scheduleRepository.save(schedule));
    }

    @Transactional
    public ScheduleResponse release(Long id) {
        Schedule schedule = getScheduleOrThrow(id);
        schedule.setStatus(ScheduleStatus.AVAILABLE);
        return scheduleMapper.toResponse(scheduleRepository.save(schedule));
    }

    @Transactional
    public void delete(Long scheduleId, Long doctorId) {
        Schedule schedule = getScheduleOrThrow(scheduleId);
        if (!schedule.getDoctorId().equals(doctorId))
            throw new BusinessException("Not authorized to delete this schedule slot");
        if (schedule.getStatus() == ScheduleStatus.BOOKED)
            throw new BusinessException("Cannot delete a booked schedule slot");
        scheduleRepository.delete(schedule);
    }

    private PersonalResponse getActiveDoctorOrThrow(Long doctorId) {
        PersonalResponse doctor = personalClient.findById(doctorId);
        if (!doctor.isActive()) throw new BusinessException("Doctor is not active");
        return doctor;
    }

    private Schedule getScheduleOrThrow(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
    }

    private void validateSlotTimes(ScheduleRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime()))
            throw new BusinessException("End time must be after start time");
    }

    private Schedule buildSchedule(PersonalResponse doctor, ScheduleRequest request) {
        return Schedule.builder()
                .doctorId(doctor.getId())
                .doctorName(doctor.getName())
                .doctorSpecialty(doctor.getSpecialtyName())
                .doctorEmail(doctor.getEmail())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
    }
}
