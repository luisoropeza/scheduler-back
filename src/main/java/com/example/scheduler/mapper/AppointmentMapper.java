package com.example.scheduler.mapper;

import com.example.scheduler.dto.appointment.AppointmentResponse;
import com.example.scheduler.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorName", source = "doctor.account.name")
    @Mapping(target = "doctorSpecialty", source = "doctor.specialty.name")
    @Mapping(target = "doctorEmail", source = "doctor.account.email")
    @Mapping(target = "clientId", source = "patient.id")
    @Mapping(target = "clientName", source = "patient.account.name")
    @Mapping(target = "clientEmail", source = "patient.account.email")
    @Mapping(target = "status", expression = "java(appointment.getStatus().getDisplayName())")
    AppointmentResponse toResponse(Appointment appointment);
}
