package com.example.scheduler.mapper;

import com.example.scheduler.dto.provider.ProviderRequest;
import com.example.scheduler.dto.provider.ProviderResponse;
import com.example.scheduler.entity.Provider;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProviderMapper {

    ProviderResponse toResponse(Provider provider);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    Provider toEntity(ProviderRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    void updateEntity(ProviderRequest request, @MappingTarget Provider provider);
}
