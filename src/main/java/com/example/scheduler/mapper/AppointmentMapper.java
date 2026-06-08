package com.example.scheduler.mapper;

import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {

    @Mapping(source = "schedule.id", target = "scheduleId")
    @Mapping(source = "schedule.startTime", target = "scheduleStart")
    @Mapping(source = "schedule.endTime", target = "scheduleEnd")
    @Mapping(source = "schedule.provider.name", target = "providerName")
    @Mapping(source = "schedule.provider.specialty", target = "providerSpecialty")
    AppointmentResponse toResponse(Appointment appointment);

    List<AppointmentResponse> toResponseList(List<Appointment> appointments);
}
