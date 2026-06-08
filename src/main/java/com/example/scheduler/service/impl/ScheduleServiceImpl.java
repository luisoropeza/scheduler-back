package com.example.scheduler.service.impl;

import com.example.scheduler.dto.schedule.ScheduleRequest;
import com.example.scheduler.dto.schedule.ScheduleResponse;
import com.example.scheduler.entity.Provider;
import com.example.scheduler.entity.Schedule;
import com.example.scheduler.entity.enums.ScheduleStatus;
import com.example.scheduler.exception.BusinessException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.ScheduleMapper;
import com.example.scheduler.repository.ProviderRepository;
import com.example.scheduler.repository.ScheduleRepository;
import com.example.scheduler.service.ScheduleService;
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
    private final ProviderRepository providerRepository;
    private final ScheduleMapper scheduleMapper;

    @Override
    public List<ScheduleResponse> findAvailableByProvider(Long providerId) {
        getActiveProviderOrThrow(providerId);
        List<Schedule> schedules = scheduleRepository
                .findByProviderIdAndStatusAndStartTimeAfterOrderByStartTimeAsc(
                        providerId, ScheduleStatus.AVAILABLE, LocalDateTime.now());
        return scheduleMapper.toResponseList(schedules);
    }

    @Override
    public List<ScheduleResponse> findAllByProvider(Long providerId) {
        getActiveProviderOrThrow(providerId);
        return scheduleMapper.toResponseList(scheduleRepository.findByProviderId(providerId));
    }

    @Override
    @Transactional
    public ScheduleResponse create(Long providerId, ScheduleRequest request) {
        Provider provider = getActiveProviderOrThrow(providerId);
        validateSlotTimes(request);
        Schedule schedule = buildSchedule(provider, request);
        return scheduleMapper.toResponse(scheduleRepository.save(schedule));
    }

    @Override
    @Transactional
    public List<ScheduleResponse> createBatch(Long providerId, List<ScheduleRequest> requests) {
        Provider provider = getActiveProviderOrThrow(providerId);
        requests.forEach(this::validateSlotTimes);
        List<Schedule> schedules = requests.stream()
                .map(req -> buildSchedule(provider, req))
                .toList();
        return scheduleMapper.toResponseList(scheduleRepository.saveAll(schedules));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + id));
        if (schedule.getStatus() == ScheduleStatus.BOOKED) {
            throw new BusinessException("Cannot delete a booked schedule slot");
        }
        scheduleRepository.delete(schedule);
    }

    private Provider getActiveProviderOrThrow(Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));
        if (!provider.isActive()) {
            throw new BusinessException("Provider is not active");
        }
        return provider;
    }

    private void validateSlotTimes(ScheduleRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException("End time must be after start time");
        }
    }

    private Schedule buildSchedule(Provider provider, ScheduleRequest request) {
        return Schedule.builder()
                .provider(provider)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(ScheduleStatus.AVAILABLE)
                .build();
    }
}
