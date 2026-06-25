package com.example.schedule.mapper;

import com.example.schedule.dto.ScheduleResponse;
import com.example.schedule.entity.Schedule;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {
    ScheduleResponse toResponse(Schedule schedule);
    List<ScheduleResponse> toResponseList(List<Schedule> schedules);
}
