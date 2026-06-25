package com.example.appointment.mapper;

import com.example.appointment.dto.AppointmentResponse;
import com.example.appointment.entity.Appointment;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    AppointmentResponse toResponse(Appointment appointment);
}
