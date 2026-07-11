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
import com.example.user.mapper.PatientMapper;
import com.example.user.mapper.PersonalMapper;
import com.example.user.repository.PatientRepository;
import com.example.user.repository.PersonalRepository;
import com.example.user.repository.RoleRepository;
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
class AuthServiceImplTest {

    @Mock private PatientRepository patientRepository;
    @Mock private PersonalRepository personalRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PatientMapper patientMapper;
    @Mock private PersonalMapper personalMapper;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role doctorRole() {
        return Role.builder().id(1L).name(ERole.DOCTOR.name()).build();
    }

    private Role receptionistRole() {
        return Role.builder().id(2L).name(ERole.RECEPTIONIST.name()).build();
    }

    private PatientRegisterRequest patientRegisterRequest() {
        PatientRegisterRequest r = new PatientRegisterRequest();
        r.setName("John Doe");
        r.setEmail("john@example.com");
        r.setPassword("password123");
        return r;
    }

    private PersonalRegisterRequest personalRegisterRequest() {
        PersonalRegisterRequest r = new PersonalRegisterRequest();
        r.setName("Dr. Smith");
        r.setEmail("dr@clinic.com");
        r.setPassword("password123");
        r.setRoleId(1L);
        return r;
    }

    private LoginRequest loginRequest(String email) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword("password123");
        return r;
    }

    // --- registerPatient ---

    @Test
    void registerPatient_newEmail_savesPatientAndReturnsToken() {
        PatientRegisterRequest request = patientRegisterRequest();
        Patient entity = Patient.builder().id(1L).build();

        when(patientRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(personalRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(patientMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(patientRepository.save(entity)).thenReturn(entity);
        when(jwtUtil.generate(1L, "PATIENT")).thenReturn("jwt-token");

        LoginResponse response = authService.registerPatient(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(entity.getPassword()).isEqualTo("hashed");
        verify(patientRepository).save(entity);
    }

    @Test
    void registerPatient_emailTakenByPatient_throwsBusinessException() {
        when(patientRepository.findByEmail("john@example.com")).thenReturn(Optional.of(new Patient()));

        assertThatThrownBy(() -> authService.registerPatient(patientRegisterRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");

        verify(patientRepository, never()).save(any());
    }

    @Test
    void registerPatient_emailTakenByPersonal_throwsBusinessException() {
        when(patientRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(personalRepository.findByEmail("john@example.com")).thenReturn(Optional.of(new Personal()));

        assertThatThrownBy(() -> authService.registerPatient(patientRegisterRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");

        verify(patientRepository, never()).save(any());
    }

    // --- loginPatient ---

    @Test
    void loginPatient_validCredentials_returnsToken() {
        Patient patient = Patient.builder().id(1L).email("john@example.com")
                .password("hashed").active(true).build();

        when(patientRepository.findByEmail("john@example.com")).thenReturn(Optional.of(patient));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtUtil.generate(1L, "PATIENT")).thenReturn("jwt-token");

        LoginResponse response = authService.loginPatient(loginRequest("john@example.com"));

        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void loginPatient_emailNotFound_throwsBusinessException() {
        when(patientRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginPatient(loginRequest("john@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void loginPatient_inactiveAccount_throwsBusinessException() {
        Patient patient = Patient.builder().id(1L).email("john@example.com")
                .password("hashed").active(false).build();
        when(patientRepository.findByEmail("john@example.com")).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> authService.loginPatient(loginRequest("john@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void loginPatient_wrongPassword_throwsBusinessException() {
        Patient patient = Patient.builder().id(1L).email("john@example.com")
                .password("hashed").active(true).build();
        when(patientRepository.findByEmail("john@example.com")).thenReturn(Optional.of(patient));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.loginPatient(loginRequest("john@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid credentials");
    }

    // --- registerPersonal ---

    @Test
    void registerPersonal_newEmail_savesPersonalAndReturnsToken() {
        PersonalRegisterRequest request = personalRegisterRequest();
        Personal entity = Personal.builder().id(1L).build();

        when(patientRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());
        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());
        when(personalMapper.toEntity(request)).thenReturn(entity);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(doctorRole()));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(personalRepository.save(entity)).thenReturn(entity);
        when(jwtUtil.generate(1L, "DOCTOR")).thenReturn("jwt-token");

        LoginResponse response = authService.registerPersonal(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(entity.getPassword()).isEqualTo("hashed");
        assertThat(entity.getRole()).isEqualTo(doctorRole());
        verify(personalRepository).save(entity);
    }

    @Test
    void registerPersonal_emailTakenByPatient_throwsBusinessException() {
        when(patientRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.of(new Patient()));

        assertThatThrownBy(() -> authService.registerPersonal(personalRegisterRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");

        verify(personalRepository, never()).save(any());
    }

    @Test
    void registerPersonal_roleNotFound_throwsResourceNotFoundException() {
        PersonalRegisterRequest request = personalRegisterRequest();
        request.setRoleId(99L);
        Personal entity = Personal.builder().id(1L).build();

        when(patientRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());
        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());
        when(personalMapper.toEntity(request)).thenReturn(entity);
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.registerPersonal(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role not found");
    }

    @Test
    void registerPersonal_receptionistWithSpecialtyId_throwsBusinessException() {
        PersonalRegisterRequest request = personalRegisterRequest();
        request.setRoleId(2L);
        request.setSpecialtyId(10L);
        Personal entity = Personal.builder().id(1L).build();

        when(patientRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());
        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());
        when(personalMapper.toEntity(request)).thenReturn(entity);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(receptionistRole()));

        assertThatThrownBy(() -> authService.registerPersonal(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not have a specialty assigned");
    }

    @Test
    void registerPersonal_doctorWithSpecialty_assignsSpecialty() {
        PersonalRegisterRequest request = personalRegisterRequest();
        request.setSpecialtyId(10L);
        Specialty specialty = Specialty.builder().id(10L).name("Cardiology").build();
        Personal entity = Personal.builder().id(1L).build();

        when(patientRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());
        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());
        when(personalMapper.toEntity(request)).thenReturn(entity);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(doctorRole()));
        when(specialtyRepository.findById(10L)).thenReturn(Optional.of(specialty));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(personalRepository.save(entity)).thenReturn(entity);
        when(jwtUtil.generate(anyLong(), anyString())).thenReturn("jwt-token");

        authService.registerPersonal(request);

        assertThat(entity.getSpecialty()).isEqualTo(specialty);
    }

    @Test
    void registerPersonal_specialtyNotFound_throwsResourceNotFoundException() {
        PersonalRegisterRequest request = personalRegisterRequest();
        request.setSpecialtyId(99L);
        Personal entity = Personal.builder().id(1L).build();

        when(patientRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());
        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());
        when(personalMapper.toEntity(request)).thenReturn(entity);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(doctorRole()));
        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.registerPersonal(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Specialty not found");
    }

    // --- loginPersonal ---

    @Test
    void loginPersonal_validCredentials_returnsToken() {
        Personal personal = Personal.builder().id(1L).email("dr@clinic.com")
                .password("hashed").role(doctorRole()).active(true).build();

        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.of(personal));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtUtil.generate(1L, "DOCTOR")).thenReturn("jwt-token");

        LoginResponse response = authService.loginPersonal(loginRequest("dr@clinic.com"));

        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void loginPersonal_emailNotFound_throwsBusinessException() {
        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginPersonal(loginRequest("dr@clinic.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void loginPersonal_inactiveAccount_throwsBusinessException() {
        Personal personal = Personal.builder().id(1L).email("dr@clinic.com")
                .password("hashed").role(doctorRole()).active(false).build();
        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.of(personal));

        assertThatThrownBy(() -> authService.loginPersonal(loginRequest("dr@clinic.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void loginPersonal_wrongPassword_throwsBusinessException() {
        Personal personal = Personal.builder().id(1L).email("dr@clinic.com")
                .password("hashed").role(doctorRole()).active(true).build();
        when(personalRepository.findByEmail("dr@clinic.com")).thenReturn(Optional.of(personal));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.loginPersonal(loginRequest("dr@clinic.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid credentials");
    }
}
