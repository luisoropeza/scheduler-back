package com.example.schedule.service;

import com.example.schedule.dto.ScheduleRequest;
import com.example.schedule.dto.ScheduleResponse;
import com.example.schedule.enums.ScheduleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleService {
    Page<ScheduleResponse> findAll(Long doctorId, String specialty, ScheduleStatus status, LocalDateTime after, Pageable pageable);
    ScheduleResponse getById(Long id);
    ScheduleResponse create(Long doctorId, ScheduleRequest request);
    List<ScheduleResponse> createBatch(Long doctorId, List<ScheduleRequest> requests);
    ScheduleResponse book(Long id);
    ScheduleResponse release(Long id);
    void delete(Long scheduleId, Long doctorId);
}
