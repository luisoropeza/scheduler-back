package com.example.user.service;

import com.example.user.dto.RoleResponse;

import java.util.List;

public interface RoleService {
    List<RoleResponse> findAll();
}
