package com.example.user.mapper;

import com.example.user.dto.RoleResponse;
import com.example.user.entity.Role;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    List<RoleResponse> toResponseList(List<Role> roles);
}
