package com.example.schedule.service;

import com.example.schedule.dto.ScheduleRequest;
import com.example.schedule.dto.ScheduleResponse;

import java.util.List;

public interface ScheduleService {
    List<ScheduleResponse> findAvailableByProvider(Long providerId);
    List<ScheduleResponse> findAllByProvider(Long providerId);
    ScheduleResponse getById(Long id);
    ScheduleResponse create(Long providerId, ScheduleRequest request);
    List<ScheduleResponse> createBatch(Long providerId, List<ScheduleRequest> requests);
    ScheduleResponse book(Long id);
    ScheduleResponse release(Long id);
    void delete(Long id);
}
