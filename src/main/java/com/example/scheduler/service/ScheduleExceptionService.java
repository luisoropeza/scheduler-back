package com.example.scheduler.service;

import com.example.scheduler.dto.ScheduleException.ScheduleExceptionRequest;
import com.example.scheduler.dto.ScheduleException.ScheduleExceptionResponse;

public interface ScheduleExceptionService {
    ScheduleExceptionResponse addDoctorScheduleException(Long doctorId, ScheduleExceptionRequest request);
}
