package com.example.scheduler.controller;

import com.example.scheduler.dto.ScheduleException.ScheduleExceptionRequest;
import com.example.scheduler.dto.ScheduleException.ScheduleExceptionResponse;
import com.example.scheduler.service.ScheduleExceptionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scheduleException")
@RequiredArgsConstructor
@Tag(name = "Schedule Exceptions", description = "Schedule Exception Controller")
public class ScheduleExceptionController {
    private final ScheduleExceptionService scheduleExceptionService;

    @PostMapping
    public ResponseEntity<ScheduleExceptionResponse> addDoctorScheduleException(@RequestBody ScheduleExceptionRequest request, Authentication auth){
        return ResponseEntity.ok(scheduleExceptionService.addDoctorScheduleException(Long.parseLong(auth.getName()), request));
    }
}
