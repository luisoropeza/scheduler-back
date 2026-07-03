package com.example.user.mapper;

import com.example.user.dto.PersonalRegisterRequest;
import com.example.user.dto.PersonalRequest;
import com.example.user.dto.PersonalResponse;
import com.example.user.entity.Personal;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PersonalMapper {
    @Mapping(target = "roleName", expression = "java(personal.getRole().name())")
    @Mapping(target = "specialtyName", source = "specialty.name")
    PersonalResponse toResponse(Personal personal);
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "specialty", ignore = true)
    @Mapping(target = "patients", ignore = true)
    Personal toEntity(PersonalRegisterRequest request);
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "specialty", ignore = true)
    @Mapping(target = "patients", ignore = true)
    @Mapping(target = "email", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toEntityUpdated(PersonalRequest request, @MappingTarget Personal personal);
    @Mapping(target = "roleName", expression = "java(personal.getRole().name())")
    @Mapping(target = "specialtyName", source = "specialty.name")
    List<PersonalResponse> toResponseList(List<Personal> personals);
}
