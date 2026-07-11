package com.example.user.service.impl;

import com.example.user.dto.PatientResponse;
import com.example.user.dto.PersonalRequest;
import com.example.user.dto.PersonalResponse;
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
import com.example.user.service.PersonalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalServiceImpl implements PersonalService {
    private final PersonalRepository personalRepository;
    private final PatientRepository patientRepository;
    private final SpecialtyRepository specialtyRepository;
    private final RoleRepository roleRepository;
    private final PersonalMapper personalMapper;
    private final PatientMapper patientMapper;

    public Page<PersonalResponse> findAll(Long specialtyId, Boolean isActive, Pageable pageable) {
        if (specialtyId != null) specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty not found: " + specialtyId));
        return personalRepository.findAllByFilters(specialtyId, isActive, pageable)
                .map(personalMapper::toResponse);
    }

    public PersonalResponse findById(Long id) {
        return personalMapper.toResponse(getOrThrow(id));
    }

    @Transactional
    public PersonalResponse update(Long id, PersonalRequest request) {
        Personal personal = getOrThrow(id);
        personalMapper.toEntityUpdated(request, personal);
        if (request.getRoleId() != null) {
            personal.setRole(getRoleOrThrow(request.getRoleId()));
        }
        if (request.getSpecialtyId() != null) {
            if(personal.getRole().getName().equals(ERole.DOCTOR.name()))
                personal.setSpecialty(getSpecialtyOrThrow(request.getSpecialtyId()));
            else
                throw new BusinessException(String.format("this %s does not have a specialty assigned", personal.getRole().getName()));
        }
        return personalMapper.toResponse(personalRepository.save(personal));
    }

    @Transactional
    public void deactivate(Long id) {
        Personal personal = getOrThrow(id);
        personal.setActive(false);
        personalRepository.save(personal);
    }

    @Transactional
    public void assignPatient(Long doctorId, Long patientId, Long userId, String role) {
        Personal doctor = getOrThrow(doctorId);
        if (role.equals(ERole.DOCTOR.name()))
            if (!doctorId.equals(userId))
                throw new BusinessException("this user cannot assign this patient");
        Patient patient = getPatientOrThrow(patientId);
        if (!doctor.getPatients().contains(patient)) {
            doctor.getPatients().add(patient);
            personalRepository.save(doctor);
        }
    }

    @Transactional
    public void removePatient(Long doctorId, Long patientId, Long userId, String role) {
        Personal doctor = getOrThrow(doctorId);
        if (role.equals(ERole.DOCTOR.name()))
            if (!doctorId.equals(userId))
                throw new BusinessException("this user cannot assign this patient");
        Patient patient = getPatientOrThrow(patientId);
        if (doctor.getPatients().contains(patient)) {
            doctor.getPatients().remove(patient);
            personalRepository.save(doctor);
        }
    }

    public List<PatientResponse> getPatientsOfDoctor(Long doctorId) {
        Personal doctor = getOrThrow(doctorId);
        return patientMapper.toResponseList(doctor.getPatients());
    }

    private Personal getOrThrow(Long id) {
        return personalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personal not found with id: " + id));
    }

    private Specialty getSpecialtyOrThrow(Long id) {
        return specialtyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty not found with id: " + id));
    }

    private Role getRoleOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
    }

    private Patient getPatientOrThrow(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
    }
}
