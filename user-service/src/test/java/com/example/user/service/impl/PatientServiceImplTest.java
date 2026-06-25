package com.example.user.service.impl;

import com.example.user.dto.PatientRequest;
import com.example.user.dto.PatientResponse;
import com.example.user.dto.PersonalResponse;
import com.example.user.entity.Patient;
import com.example.user.entity.Personal;
import com.example.user.exception.ResourceNotFoundException;
import com.example.user.mapper.PatientMapper;
import com.example.user.mapper.PersonalMapper;
import com.example.user.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock private PatientRepository patientRepository;
    @Mock private PatientMapper patientMapper;
    @Mock private PersonalMapper personalMapper;

    @InjectMocks
    private PatientServiceImpl patientService;

    private Patient activePatient() {
        return Patient.builder().id(1L).name("John").email("john@example.com").active(true).build();
    }

    // --- findAll ---

    @Test
    void findAll_returnsPageOfPatients() {
        Patient patient = activePatient();
        PatientResponse response = new PatientResponse();
        when(patientRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(patient)));
        when(patientMapper.toResponse(patient)).thenReturn(response);

        Page<PatientResponse> result = patientService.findAll(Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findAll_emptyRepository_returnsEmptyPage() {
        when(patientRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        Page<PatientResponse> result = patientService.findAll(Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    // --- findById ---

    @Test
    void findById_existingId_returnsResponse() {
        Patient patient = activePatient();
        PatientResponse response = new PatientResponse();
        response.setId(1L);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientMapper.toResponse(patient)).thenReturn(response);

        PatientResponse result = patientService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- update ---

    @Test
    void update_appliesMapperAndSaves() {
        Patient patient = activePatient();
        PatientRequest request = new PatientRequest();
        request.setName("Updated Name");
        PatientResponse response = new PatientResponse();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientRepository.save(patient)).thenReturn(patient);
        when(patientMapper.toResponse(patient)).thenReturn(response);

        patientService.update(1L, request);

        verify(patientMapper).toEntityUpdated(request, patient);
        verify(patientRepository).save(patient);
    }

    @Test
    void update_notFound_throwsResourceNotFoundException() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.update(99L, new PatientRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- deactivate ---

    @Test
    void deactivate_setsActiveFalseAndSaves() {
        Patient patient = activePatient();
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        patientService.deactivate(1L);

        assertThat(patient.isActive()).isFalse();
        verify(patientRepository).save(patient);
    }

    @Test
    void deactivate_notFound_throwsResourceNotFoundException() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.deactivate(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- getDoctorsOfPatient ---

    @Test
    void getDoctorsOfPatient_returnsListOfDoctors() {
        Personal doctor = Personal.builder().id(10L).name("Dr. Smith").build();
        Patient patient = Patient.builder().id(1L).doctors(new ArrayList<>(List.of(doctor))).build();
        PersonalResponse response = new PersonalResponse();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(personalMapper.toResponseList(List.of(doctor))).thenReturn(List.of(response));

        List<PersonalResponse> result = patientService.getDoctorsOfPatient(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getDoctorsOfPatient_noDoctors_returnsEmptyList() {
        Patient patient = Patient.builder().id(1L).build();

        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(personalMapper.toResponseList(anyList())).thenReturn(List.of());

        List<PersonalResponse> result = patientService.getDoctorsOfPatient(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getDoctorsOfPatient_notFound_throwsResourceNotFoundException() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getDoctorsOfPatient(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
