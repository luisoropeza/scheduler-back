package com.example.user.mapper;

import com.example.user.dto.PatientRegisterRequest;
import com.example.user.dto.PatientRequest;
import com.example.user.dto.PatientResponse;
import com.example.user.entity.Patient;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PatientMapper {
    PatientResponse toResponse(Patient patient);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "doctors", ignore = true)
    @Mapping(target = "password", ignore = true)
    Patient toEntity(PatientRegisterRequest request);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "doctors", ignore = true)
    @Mapping(target = "password", ignore = true)
    void toEntityUpdated(PatientRequest request, @MappingTarget Patient patient);
    List<PatientResponse> toResponseList(List<Patient> patients);
}
