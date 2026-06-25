package com.example.user.service;

import com.example.user.dto.LoginRequest;
import com.example.user.dto.LoginResponse;
import com.example.user.dto.PersonalRegisterRequest;
import com.example.user.entity.Personal;
import com.example.user.entity.Specialty;
import com.example.user.enums.ERole;
import com.example.user.exception.BusinessException;
import com.example.user.exception.ResourceNotFoundException;
import com.example.user.mapper.PersonalMapper;
import com.example.user.repository.PersonalRepository;
import com.example.user.repository.SpecialtyRepository;
import com.example.user.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonalAuthService {
    private final PersonalRepository personalRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PersonalMapper personalMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public LoginResponse register(PersonalRegisterRequest request) {
        if (personalRepository.findByEmail(request.getEmail()).isPresent())
            throw new BusinessException("Email already registered");
        if (request.getRole() == ERole.RECEPTIONIST && request.getSpecialtyId() != null)
            throw new BusinessException("Receptionists cannot have a specialty assigned");
        Specialty specialty = request.getSpecialtyId() != null
                ? specialtyRepository.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Specialty not found: " + request.getSpecialtyId()))
                : null;
        Personal personal = personalMapper.toEntity(request);
        personal.setPassword(passwordEncoder.encode(request.getPassword()));
        personal.setSpecialty(specialty);
        Personal saved = personalRepository.save(personal);
        return new LoginResponse(jwtUtil.generate(saved.getId(), request.getRole().name()));
    }

    public LoginResponse login(LoginRequest request) {
        Personal personal = personalRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));
        if (!personal.isActive()) throw new BusinessException("Account is inactive");
        if (!passwordEncoder.matches(request.getPassword(), personal.getPassword())) {
            throw new BusinessException("Invalid credentials");
        }
        return new LoginResponse(jwtUtil.generate(personal.getId(), personal.getRole().name()));
    }
}
