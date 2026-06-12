package com.example.schedule.service.impl;

import com.example.schedule.client.ProviderClient;
import com.example.schedule.dto.ProviderResponse;
import com.example.schedule.dto.ScheduleRequest;
import com.example.schedule.dto.ScheduleResponse;
import com.example.schedule.entity.Schedule;
import com.example.schedule.entity.enums.ScheduleStatus;
import com.example.schedule.exception.BusinessException;
import com.example.schedule.exception.ResourceNotFoundException;
import com.example.schedule.mapper.ScheduleMapper;
import com.example.schedule.repository.ScheduleRepository;
import com.example.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ProviderClient providerClient;
    private final ScheduleMapper scheduleMapper;

    @Override
    public List<ScheduleResponse> findAvailableByProvider(Long providerId) {
        getActiveProviderOrThrow(providerId);
        return scheduleMapper.toResponseList(
                scheduleRepository.findByProviderIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
                        providerId, ScheduleStatus.AVAILABLE, LocalDateTime.now()));
    }

    @Override
    public List<ScheduleResponse> findAllByProvider(Long providerId) {
        getActiveProviderOrThrow(providerId);
        return scheduleMapper.toResponseList(scheduleRepository.findByProviderId(providerId));
    }

    @Override
    public ScheduleResponse getById(Long id) {
        return scheduleMapper.toResponse(getScheduleOrThrow(id));
    }

    @Override
    @Transactional
    public ScheduleResponse create(Long providerId, ScheduleRequest request) {
        ProviderResponse provider = getActiveProviderOrThrow(providerId);
        validateSlotTimes(request);
        return scheduleMapper.toResponse(scheduleRepository.save(buildSchedule(provider, request)));
    }

    @Override
    @Transactional
    public List<ScheduleResponse> createBatch(Long providerId, List<ScheduleRequest> requests) {
        ProviderResponse provider = getActiveProviderOrThrow(providerId);
        requests.forEach(this::validateSlotTimes);
        List<Schedule> schedules = requests.stream().map(r -> buildSchedule(provider, r)).toList();
        return scheduleMapper.toResponseList(scheduleRepository.saveAll(schedules));
    }

    @Override
    @Transactional
    public ScheduleResponse book(Long id) {
        Schedule schedule = getScheduleOrThrow(id);
        if (schedule.getStatus() != ScheduleStatus.AVAILABLE) {
            throw new BusinessException("Schedule slot is not available");
        }
        schedule.setStatus(ScheduleStatus.BOOKED);
        return scheduleMapper.toResponse(scheduleRepository.save(schedule));
    }

    @Override
    @Transactional
    public ScheduleResponse release(Long id) {
        Schedule schedule = getScheduleOrThrow(id);
        schedule.setStatus(ScheduleStatus.AVAILABLE);
        return scheduleMapper.toResponse(scheduleRepository.save(schedule));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Schedule schedule = getScheduleOrThrow(id);
        if (schedule.getStatus() == ScheduleStatus.BOOKED) {
            throw new BusinessException("Cannot delete a booked schedule slot");
        }
        scheduleRepository.delete(schedule);
    }

    private ProviderResponse getActiveProviderOrThrow(Long providerId) {
        ProviderResponse provider = providerClient.findById(providerId);
        if (!provider.isActive()) {
            throw new BusinessException("Provider is not active");
        }
        return provider;
    }

    private Schedule getScheduleOrThrow(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
    }

    private void validateSlotTimes(ScheduleRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("End time must be after start time");
        }
    }

    private Schedule buildSchedule(ProviderResponse provider, ScheduleRequest request) {
        return Schedule.builder()
                .providerId(provider.getId())
                .providerName(provider.getName())
                .providerSpecialty(provider.getSpecialty())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
    }
}
