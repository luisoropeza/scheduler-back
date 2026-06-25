package com.example.schedule.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleRequest {
    @NotNull
    @Future
    private LocalDateTime startTime;
    @NotNull
    @Future
    private LocalDateTime endTime;
}
