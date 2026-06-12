package com.example.provider.mapper;

import com.example.provider.dto.ProviderRequest;
import com.example.provider.dto.ProviderResponse;
import com.example.provider.entity.Provider;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProviderMapper {

    ProviderResponse toResponse(Provider provider);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    Provider toEntity(ProviderRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntity(ProviderRequest request, @MappingTarget Provider provider);
}
