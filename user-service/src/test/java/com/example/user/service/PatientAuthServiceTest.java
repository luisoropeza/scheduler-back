package com.example.user.service;

import com.example.user.dto.LoginRequest;
import com.example.user.dto.LoginResponse;
import com.example.user.dto.PatientRegisterRequest;
import com.example.user.entity.Patient;
import com.example.user.exception.BusinessException;
import com.example.user.mapper.PatientMapper;
import com.example.user.repository.PatientRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientAuthServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private PatientMapper patientMapper;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private PatientAuthService patientAuthService;

    private PatientRegisterRequest registerRequest() {
        PatientRegisterRequest r = new PatientRegisterRequest();
        r.setName("John Doe");
        r.setEmail("john@example.com");
        r.setPassword("password123");
        return r;
    }

    private LoginRequest loginRequest() {
        LoginRequest r = new LoginRequest();
        r.setEmail("john@example.com");
        r.setPassword("password123");
        return r;
    }

    // --- register ---

    @Test
    void register_newEmail_savesPatientAndReturnsToken() {
        PatientRegisterRequest request = registerRequest();
        Patient entity = Patient.builder().id(1L).build();

        when(patientRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(patientMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(patientRepository.save(entity)).thenReturn(entity);
        when(jwtUtil.generate(1L, "PATIENT")).thenReturn("jwt-token");

        LoginResponse response = patientAuthService.register(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(entity.getPassword()).isEqualTo("hashed");
        verify(patientRepository).save(entity);
    }

    @Test
    void register_duplicateEmail_throwsBusinessException() {
        when(patientRepository.findByEmail("john@example.com")).thenReturn(Optional.of(new Patient()));

        assertThatThrownBy(() -> patientAuthService.register(registerRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");

        verify(patientRepository, never()).save(any());
    }

    // --- login ---

    @Test
    void login_validCredentials_returnsToken() {
        Patient patient = Patient.builder().id(1L).email("john@example.com")
                .password("hashed").active(true).build();

        when(patientRepository.findByEmail("john@example.com")).thenReturn(Optional.of(patient));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtUtil.generate(1L, "PATIENT")).thenReturn("jwt-token");

        LoginResponse response = patientAuthService.login(loginRequest());

        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_emailNotFound_throwsBusinessException() {
        when(patientRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientAuthService.login(loginRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_inactiveAccount_throwsBusinessException() {
        Patient patient = Patient.builder().id(1L).email("john@example.com")
                .password("hashed").active(false).build();
        when(patientRepository.findByEmail("john@example.com")).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> patientAuthService.login(loginRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void login_wrongPassword_throwsBusinessException() {
        Patient patient = Patient.builder().id(1L).email("john@example.com")
                .password("hashed").active(true).build();

        when(patientRepository.findByEmail("john@example.com")).thenReturn(Optional.of(patient));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> patientAuthService.login(loginRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid credentials");
    }
}
