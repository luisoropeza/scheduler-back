package com.example.scheduler.mapper;

import com.example.scheduler.dto.schedule.ScheduleRequest;
import com.example.scheduler.dto.schedule.ScheduleResponse;
import com.example.scheduler.entity.Schedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {

    @Mapping(source = "provider.id", target = "providerId")
    @Mapping(source = "provider.name", target = "providerName")
    @Mapping(source = "provider.specialty", target = "providerSpecialty")
    ScheduleResponse toResponse(Schedule schedule);

    List<ScheduleResponse> toResponseList(List<Schedule> schedules);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "provider", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "appointment", ignore = true)
    Schedule toEntity(ScheduleRequest request);
}
