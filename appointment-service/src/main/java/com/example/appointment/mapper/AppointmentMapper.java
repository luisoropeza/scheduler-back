package com.example.appointment.mapper;

import com.example.appointment.dto.AppointmentResponse;
import com.example.appointment.entity.Appointment;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    // All fields are direct on the entity — no @Mapping annotations needed.
    AppointmentResponse toResponse(Appointment appointment);
    List<AppointmentResponse> toResponseList(List<Appointment> appointments);
}
