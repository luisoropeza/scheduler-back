package com.example.user.service;

import com.example.user.enums.ERole;
import com.example.user.exception.BusinessException;
import com.example.user.security.JwtUtil;
import com.example.user.dto.LoginRequest;
import com.example.user.dto.LoginResponse;
import com.example.user.dto.PatientRegisterRequest;
import com.example.user.entity.Patient;
import com.example.user.mapper.PatientMapper;
import com.example.user.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientAuthService {
    private final PatientRepository patientRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PatientMapper patientMapper;
    private final JwtUtil jwtUtil;

    @Transactional
    public LoginResponse register(PatientRegisterRequest request) {
        if (patientRepository.findByEmail(request.getEmail()).isPresent())
            throw new BusinessException("Email already registered");
        Patient patient = patientMapper.toEntity(request);
        patient.setPassword(passwordEncoder.encode(request.getPassword()));
        Patient saved = patientRepository.save(patient);
        return new LoginResponse(jwtUtil.generate(saved.getId(), ERole.PATIENT.name()));
    }

    public LoginResponse login(LoginRequest request) {
        Patient patient = patientRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));
        if (!patient.isActive()) throw new BusinessException("Account is inactive");
        if (!passwordEncoder.matches(request.getPassword(), patient.getPassword())) {
            throw new BusinessException("Invalid credentials");
        }
        return new LoginResponse(jwtUtil.generate(patient.getId(), ERole.PATIENT.name()));
    }
}
