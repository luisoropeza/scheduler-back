package com.example.user.service.impl;

import com.example.user.dto.PatientResponse;
import com.example.user.dto.PersonalRequest;
import com.example.user.dto.PersonalResponse;
import com.example.user.entity.Patient;
import com.example.user.entity.Personal;
import com.example.user.entity.Specialty;
import com.example.user.enums.ERole;
import com.example.user.exception.BusinessException;
import com.example.user.exception.ResourceNotFoundException;
import com.example.user.mapper.PatientMapper;
import com.example.user.mapper.PersonalMapper;
import com.example.user.repository.PatientRepository;
import com.example.user.repository.PersonalRepository;
import com.example.user.repository.SpecialtyRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalServiceImplTest {

    @Mock private PersonalRepository personalRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private SpecialtyRepository specialtyRepository;
    @Mock private PersonalMapper personalMapper;
    @Mock private PatientMapper patientMapper;

    @InjectMocks
    private PersonalServiceImpl personalService;

    private Specialty cardiology() {
        return Specialty.builder().id(10L).name("Cardiology").build();
    }

    private Personal activeDoctor() {
        return Personal.builder().id(1L).name("Dr. Smith").email("dr@clinic.com")
                .role(ERole.DOCTOR).active(true).build();
    }

    // --- findAll ---

    @Test
    void findAll_noFilters_returnsPage() {
        Personal p = activeDoctor();
        PersonalResponse response = new PersonalResponse();
        when(personalRepository.findAllByFilters(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));
        when(personalMapper.toResponse(p)).thenReturn(response);

        Page<PersonalResponse> result = personalService.findAll(null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verifyNoInteractions(specialtyRepository);
    }

    @Test
    void findAll_validSpecialtyId_validatesSpecialtyExistence() {
        when(specialtyRepository.findById(10L)).thenReturn(Optional.of(cardiology()));
        when(personalRepository.findAllByFilters(eq(10L), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        personalService.findAll(10L, null, Pageable.unpaged());

        verify(specialtyRepository).findById(10L);
    }

    @Test
    void findAll_invalidSpecialtyId_throwsResourceNotFoundException() {
        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personalService.findAll(99L, null, Pageable.unpaged()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAll_activeFilter_passedToRepository() {
        when(personalRepository.findAllByFilters(isNull(), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        personalService.findAll(null, true, Pageable.unpaged());

        verify(personalRepository).findAllByFilters(isNull(), eq(true), any(Pageable.class));
    }

    // --- findById ---

    @Test
    void findById_existingId_returnsResponse() {
        Personal p = activeDoctor();
        PersonalResponse response = new PersonalResponse();
        response.setId(1L);
        when(personalRepository.findById(1L)).thenReturn(Optional.of(p));
        when(personalMapper.toResponse(p)).thenReturn(response);

        PersonalResponse result = personalService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(personalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personalService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- update ---

    @Test
    void update_noRoleOrSpecialtyChange_savesWithExistingRole() {
        Personal personal = activeDoctor();
        PersonalRequest request = new PersonalRequest();
        request.setName("Updated Name");

        when(personalRepository.findById(1L)).thenReturn(Optional.of(personal));
        when(personalRepository.save(personal)).thenReturn(personal);
        when(personalMapper.toResponse(personal)).thenReturn(new PersonalResponse());

        personalService.update(1L, request);

        verify(personalRepository).save(personal);
        assertThat(personal.getRole()).isEqualTo(ERole.DOCTOR);
    }

    @Test
    void update_withRole_setsNewRole() {
        Personal personal = activeDoctor();
        PersonalRequest request = new PersonalRequest();
        request.setRole(ERole.RECEPTIONIST);

        when(personalRepository.findById(1L)).thenReturn(Optional.of(personal));
        when(personalRepository.save(personal)).thenReturn(personal);
        when(personalMapper.toResponse(personal)).thenReturn(new PersonalResponse());

        personalService.update(1L, request);

        assertThat(personal.getRole()).isEqualTo(ERole.RECEPTIONIST);
        assertThat(personal.getSpecialty()).isNull();
    }

    @Test
    void update_switchToReceptionist_clearsSpecialty() {
        Personal personal = Personal.builder().id(1L).role(ERole.DOCTOR).specialty(cardiology()).active(true).build();
        PersonalRequest request = new PersonalRequest();
        request.setRole(ERole.RECEPTIONIST);

        when(personalRepository.findById(1L)).thenReturn(Optional.of(personal));
        when(personalRepository.save(personal)).thenReturn(personal);
        when(personalMapper.toResponse(personal)).thenReturn(new PersonalResponse());

        personalService.update(1L, request);

        assertThat(personal.getSpecialty()).isNull();
    }

    @Test
    void update_withSpecialtyId_loadsAndSetsNewSpecialty() {
        Personal personal = activeDoctor();
        Specialty specialty = cardiology();
        PersonalRequest request = new PersonalRequest();
        request.setSpecialtyId(10L);

        when(personalRepository.findById(1L)).thenReturn(Optional.of(personal));
        when(specialtyRepository.findById(10L)).thenReturn(Optional.of(specialty));
        when(personalRepository.save(personal)).thenReturn(personal);
        when(personalMapper.toResponse(personal)).thenReturn(new PersonalResponse());

        personalService.update(1L, request);

        assertThat(personal.getSpecialty()).isEqualTo(specialty);
    }

    @Test
    void update_specialtyNotFound_throwsResourceNotFoundException() {
        Personal personal = activeDoctor();
        PersonalRequest request = new PersonalRequest();
        request.setSpecialtyId(99L);

        when(personalRepository.findById(1L)).thenReturn(Optional.of(personal));
        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personalService.update(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- deactivate ---

    @Test
    void deactivate_setsActiveFalseAndSaves() {
        Personal personal = activeDoctor();
        when(personalRepository.findById(1L)).thenReturn(Optional.of(personal));

        personalService.deactivate(1L);

        assertThat(personal.isActive()).isFalse();
        verify(personalRepository).save(personal);
    }

    @Test
    void deactivate_notFound_throwsResourceNotFoundException() {
        when(personalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personalService.deactivate(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- assignPatient ---

    @Test
    void assignPatient_doctor_addsPatientToList() {
        Personal doctor = activeDoctor();
        Patient patient = Patient.builder().id(2L).name("Jane").build();

        when(personalRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));

        personalService.assignPatient(1L, 2L);

        assertThat(doctor.getPatients()).contains(patient);
        verify(personalRepository).save(doctor);
    }

    @Test
    void assignPatient_alreadyAssigned_doesNotDuplicateOrSave() {
        Patient patient = Patient.builder().id(2L).name("Jane").build();
        Personal doctor = Personal.builder().id(1L).role(ERole.DOCTOR)
                .patients(new ArrayList<>(List.of(patient))).build();

        when(personalRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));

        personalService.assignPatient(1L, 2L);

        assertThat(doctor.getPatients()).hasSize(1);
        verify(personalRepository, never()).save(any());
    }

    @Test
    void assignPatient_nonDoctor_throwsBusinessException() {
        Personal receptionist = Personal.builder().id(1L).role(ERole.RECEPTIONIST).build();
        when(personalRepository.findById(1L)).thenReturn(Optional.of(receptionist));

        assertThatThrownBy(() -> personalService.assignPatient(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only doctors");
    }

    @Test
    void assignPatient_patientNotFound_throwsResourceNotFoundException() {
        when(personalRepository.findById(1L)).thenReturn(Optional.of(activeDoctor()));
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personalService.assignPatient(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- removePatient ---

    @Test
    void removePatient_removesPatientFromListAndSaves() {
        Patient patient = Patient.builder().id(2L).name("Jane").build();
        Personal doctor = Personal.builder().id(1L).role(ERole.DOCTOR)
                .patients(new ArrayList<>(List.of(patient))).build();

        when(personalRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));

        personalService.removePatient(1L, 2L);

        assertThat(doctor.getPatients()).doesNotContain(patient);
        verify(personalRepository).save(doctor);
    }

    @Test
    void removePatient_patientNotFound_throwsResourceNotFoundException() {
        when(personalRepository.findById(1L)).thenReturn(Optional.of(activeDoctor()));
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personalService.removePatient(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- getPatientsOfDoctor ---

    @Test
    void getPatientsOfDoctor_returnsPatientList() {
        Patient patient = Patient.builder().id(2L).name("Jane").build();
        Personal doctor = Personal.builder().id(1L).role(ERole.DOCTOR)
                .patients(new ArrayList<>(List.of(patient))).build();
        PatientResponse response = new PatientResponse();

        when(personalRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientMapper.toResponseList(List.of(patient))).thenReturn(List.of(response));

        List<PatientResponse> result = personalService.getPatientsOfDoctor(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getPatientsOfDoctor_notFound_throwsResourceNotFoundException() {
        when(personalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personalService.getPatientsOfDoctor(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
