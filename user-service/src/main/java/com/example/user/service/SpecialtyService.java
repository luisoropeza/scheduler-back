package com.example.user.service;

import com.example.user.dto.SpecialtyResponse;

import java.util.List;

public interface SpecialtyService {
    List<SpecialtyResponse> findAll();
}
