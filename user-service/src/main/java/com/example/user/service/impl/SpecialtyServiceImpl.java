package com.example.user.service.impl;

import com.example.user.dto.SpecialtyResponse;
import com.example.user.mapper.SpecialtyMapper;
import com.example.user.repository.SpecialtyRepository;
import com.example.user.service.SpecialtyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpecialtyServiceImpl implements SpecialtyService {
    private final SpecialtyRepository specialtyRepository;
    private final SpecialtyMapper specialtyMapper;

    @Override
    public List<SpecialtyResponse> findAll() {
        return specialtyMapper.toResponseList(specialtyRepository.findAll());
    }
}
