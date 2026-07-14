package com.example.user.service.impl;

import com.example.user.dto.LoginRequest;
import com.example.user.dto.LoginResponse;
import com.example.user.dto.PatientRegisterRequest;
import com.example.user.dto.PersonalRegisterRequest;
import com.example.user.entity.Patient;
import com.example.user.entity.Personal;
import com.example.user.entity.Role;
import com.example.user.entity.Specialty;
import com.example.user.enums.ERole;
import com.example.user.exception.BusinessException;
import com.example.user.exception.ResourceNotFoundException;
import com.example.user.exception.UnauthorizedException;
import com.example.user.mapper.PatientMapper;
import com.example.user.mapper.PersonalMapper;
import com.example.user.repository.PatientRepository;
import com.example.user.repository.PersonalRepository;
import com.example.user.repository.RoleRepository;
import com.example.user.repository.SpecialtyRepository;
import com.example.user.security.JwtUtil;
import com.example.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    public static final String PATIENT_ROLE = "PATIENT";

    private final PatientRepository patientRepository;
    private final PersonalRepository personalRepository;
    private final SpecialtyRepository specialtyRepository;
    private final RoleRepository roleRepository;
    private final PatientMapper patientMapper;
    private final PersonalMapper personalMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public LoginResponse registerPatient(PatientRegisterRequest request) {
        if (patientRepository.findByEmail(request.getEmail()).isPresent() || personalRepository.findByEmail(request.getEmail()).isPresent())
            throw new BusinessException("Email already registered");
        Patient patient = patientMapper.toEntity(request);
        patient.setPassword(passwordEncoder.encode(request.getPassword()));
        return new LoginResponse(jwtUtil.generate(patientRepository.save(patient).getId(), PATIENT_ROLE));
    }

    @Override
    public LoginResponse loginPatient(LoginRequest request) {
        Patient patient = patientRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!patient.isActive()) throw new UnauthorizedException("Account is inactive");
        if (!passwordEncoder.matches(request.getPassword(), patient.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        return new LoginResponse(jwtUtil.generate(patient.getId(), PATIENT_ROLE));
    }

    @Override
    @Transactional
    public LoginResponse registerPersonal(PersonalRegisterRequest request) {
        if (patientRepository.findByEmail(request.getEmail()).isPresent() || personalRepository.findByEmail(request.getEmail()).isPresent())
            throw new BusinessException("Email already registered");
        Personal personal = personalMapper.toEntity(request);
        Role role = getRoleOrThrow(request.getRoleId());
        if (request.getSpecialtyId() != null)
            if (role.getName().equals(ERole.DOCTOR.name()))
                personal.setSpecialty(getSpecialtyOrThrow(request.getSpecialtyId()));
            else
                throw new BusinessException(String.format("this %s does not have a specialty assigned", role.getName()));
        personal.setPassword(passwordEncoder.encode(request.getPassword()));
        personal.setRole(role);
        return new LoginResponse(jwtUtil.generate(personalRepository.save(personal).getId(), role.getName()));
    }

    @Override
    public LoginResponse loginPersonal(LoginRequest request) {
        Personal personal = personalRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!personal.isActive()) throw new UnauthorizedException("Account is inactive");
        if (!passwordEncoder.matches(request.getPassword(), personal.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        return new LoginResponse(jwtUtil.generate(personal.getId(), personal.getRole().getName()));
    }

    private Specialty getSpecialtyOrThrow(Long id) {
        return specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty not found with id: " + id));
    }

    private Role getRoleOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
    }
}
