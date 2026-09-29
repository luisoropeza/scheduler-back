package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.login.LoginRequest;
import com.example.scheduler.dto.login.LoginResponse;
import com.example.scheduler.exception.UnauthorizedException;
import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.security.JwtUtil;
import com.example.scheduler.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final PatientRepository patientRepository;
    private final PersonalRepository personalRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        var schemaName = "clinic_" + request.clinicId();
        try {
            TenantContext.setCurrentTenant(schemaName);
            var personal = personalRepository.findByAccountEmail(request.email()).orElse(null);
            var patient = patientRepository.findByAccountEmail(request.email()).orElse(null);
            if (personal == null && patient == null) {
                throw new UnauthorizedException("Invalid Credentials");
            }
            var account = personal != null ? personal.getAccount() : patient.getAccount();
            var id = personal != null ? personal.getId() : patient.getId();
            var role = personal != null ? personal.getRole() : patient.getRole();
            if (!passwordEncoder.matches(request.password(), account.getPassword())) {
                throw new UnauthorizedException("Invalid Credentials");
            }
            return new LoginResponse(jwtUtil.generate(id, role.getName().name(), request.clinicId(), account.getName()));
        } finally {
            TenantContext.clear();
        }
    }
}
