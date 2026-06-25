package com.example.user.mapper;

import com.example.user.dto.SpecialtyResponse;
import com.example.user.entity.Specialty;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SpecialtyMapper {
    List<SpecialtyResponse> toResponseList(List<Specialty> specialties);
}
