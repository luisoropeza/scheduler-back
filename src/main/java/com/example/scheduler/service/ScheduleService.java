package com.example.scheduler.service;

import com.example.scheduler.dto.schedule.ScheduleRequest;
import com.example.scheduler.dto.schedule.ScheduleResponse;

import java.util.List;

public interface ScheduleService {

    List<ScheduleResponse> findAvailableByProvider(Long providerId);

    List<ScheduleResponse> findAllByProvider(Long providerId);

    ScheduleResponse create(Long providerId, ScheduleRequest request);

    List<ScheduleResponse> createBatch(Long providerId, List<ScheduleRequest> requests);

    void delete(Long id);
}
