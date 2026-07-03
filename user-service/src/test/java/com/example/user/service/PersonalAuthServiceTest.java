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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalAuthServiceTest {

    @Mock private PersonalRepository personalRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private PersonalMapper personalMapper;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private PersonalAuthService personalAuthService;

    private PersonalRegisterRequest registerRequest() {
        PersonalRegisterRequest r = new PersonalRegisterRequest();
        r.setName("Dr. Smith");
        r.setEmail("dr@clinic.com");
        r.setPassword("password123");
        r.setRole(ERole.DOCTOR);
        return r;
    }

    private LoginRequest loginRequest() {
        LoginRequest r = new LoginRequest();
        r.setEmail("dr@clinic.com");
        r.setPassword("password123");
        return r;
    }

    // --- register ---

    @Test
    void register_newEmail_savesPersonalAndReturnsToken() {
        PersonalRegisterRequest request = registerRequest();
        Personal entity = Personal.builder().id(1L).role(ERole.DOCTOR).build();

        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());
        when(personalMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(personalRepository.save(entity)).thenReturn(entity);
        when(jwtUtil.generate(1L, "DOCTOR")).thenReturn("jwt-token");

        LoginResponse response = personalAuthService.register(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(entity.getPassword()).isEqualTo("hashed");
        verify(personalRepository).save(entity);
    }

    @Test
    void register_duplicateEmail_throwsBusinessException() {
        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.of(new Personal()));

        assertThatThrownBy(() -> personalAuthService.register(registerRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");

        verify(personalRepository, never()).save(any());
    }

    @Test
    void register_receptionistWithSpecialtyId_throwsBusinessException() {
        PersonalRegisterRequest request = registerRequest();
        request.setRole(ERole.RECEPTIONIST);
        request.setSpecialtyId(10L);

        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personalAuthService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Receptionists cannot have a specialty");
    }

    @Test
    void register_doctorWithSpecialty_assignsSpecialty() {
        PersonalRegisterRequest request = registerRequest();
        request.setSpecialtyId(10L);
        Specialty specialty = Specialty.builder().id(10L).name("Cardiology").build();
        Personal entity = Personal.builder().id(1L).role(ERole.DOCTOR).build();

        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());
        when(specialtyRepository.findById(10L)).thenReturn(Optional.of(specialty));
        when(personalMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(personalRepository.save(entity)).thenReturn(entity);
        when(jwtUtil.generate(anyLong(), anyString())).thenReturn("jwt-token");

        personalAuthService.register(request);

        assertThat(entity.getSpecialty()).isEqualTo(specialty);
    }

    @Test
    void register_specialtyNotFound_throwsResourceNotFoundException() {
        PersonalRegisterRequest request = registerRequest();
        request.setSpecialtyId(99L);

        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());
        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personalAuthService.register(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Specialty not found");
    }

    // --- login ---

    @Test
    void login_validCredentials_returnsToken() {
        Personal personal = Personal.builder().id(1L).email("dr@clinic.com")
                .password("hashed").role(ERole.DOCTOR).active(true).build();

        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.of(personal));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtUtil.generate(1L, "DOCTOR")).thenReturn("jwt-token");

        LoginResponse response = personalAuthService.login(loginRequest());

        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_emailNotFound_throwsBusinessException() {
        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personalAuthService.login(loginRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_inactiveAccount_throwsBusinessException() {
        Personal personal = Personal.builder().id(1L).email("dr@clinic.com")
                .password("hashed").role(ERole.DOCTOR).active(false).build();
        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.of(personal));

        assertThatThrownBy(() -> personalAuthService.login(loginRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void login_wrongPassword_throwsBusinessException() {
        Personal personal = Personal.builder().id(1L).email("dr@clinic.com")
                .password("hashed").role(ERole.DOCTOR).active(true).build();

        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.of(personal));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> personalAuthService.login(loginRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid credentials");
    }
}
