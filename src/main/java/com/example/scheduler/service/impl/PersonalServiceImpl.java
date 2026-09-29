package com.example.scheduler.service.impl;

import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.AssignAndRemoveRequest;
import com.example.scheduler.dto.personal.PersonalRegisterRequest;
import com.example.scheduler.dto.personal.PersonalRequest;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.entity.Patient;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.entity.Role;
import com.example.scheduler.entity.Specialty;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.ForbiddenException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.PatientMapper;
import com.example.scheduler.mapper.PersonalMapper;
import com.example.scheduler.repository.*;
import com.example.scheduler.service.PersonalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private final AccountRepository accountRepository;
    private final PersonalMapper personalMapper;
    private final PatientMapper patientMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public Page<PersonalResponse> findAllDoctors(Long specialtyId, Boolean isActive, Pageable pageable) {
        if(specialtyId != null) getSpecialtyOrThrowById(specialtyId);
        return personalRepository.findAllDoctorsByFilters(specialtyId, isActive, pageable)
                .map(personalMapper::toResponse);
    }

    @Override
    public Page<PersonalResponse> findAllPersonal(Long specialtyId, Boolean isActive, Long roleId, Pageable pageable) {
        if(specialtyId != null) getSpecialtyOrThrowById(specialtyId);
        if(roleId != null) getRoleOrThrowById(roleId);
        return personalRepository.findAllByFilters(specialtyId, isActive, roleId, pageable)
                .map(personalMapper::toResponse);
    }

    @Override
    @Transactional
    public PersonalResponse createPersonal(PersonalRegisterRequest request) {
        var personal = personalRepository.findByAccountCi(request.ci())
                .orElseGet(() -> {
                    if(accountRepository.existsByEmailOrCi(request.email(), request.ci()))
                        throw new BadRequestException("Account with email " + request.email() + " or ci"+ request.ci() +" already exists");
                    return personalMapper.toEntity(request);
                });
        var role = getRoleOrThrowById(request.roleId());
        personal.setRole(role);
        personal.getAccount().setPassword(passwordEncoder.encode(request.password()));
        var specialty = getSpecialtyOrThrowById(request.specialtyId());
        personal.setSpecialty(specialty);
        return personalMapper.toResponse(personalRepository.save(personal));
    }

    @Override
    public PersonalResponse findPersonalById(Long personalId) {
        return personalMapper.toResponse(getPersonalOrThrowById(personalId));
    }

    @Override
    @Transactional
    public PersonalResponse updatePersonalById(Long personalId, PersonalRequest request) {
        var personal = getPersonalOrThrowById(personalId);
        personalMapper.toEntityUpdated(request, personal);
        return personalMapper.toResponse(personalRepository.save(personal));
    }

    @Override
    @Transactional
    public void deactivatePersonalById(Long personalId) {
        var personal = getPersonalOrThrowById(personalId);
        personal.setActive(false);
        personalRepository.save(personal);
    }

    @Override
    @Transactional
    public void assignPatient(AssignAndRemoveRequest request, Long userId, String role) {
        var doctor = getPersonalPatientsOrThrowById(request.doctorId());
        var patient = getPatientOrThrowById(request.patientId());
        verifyDoctorPermission(role, doctor.getId(), userId);
        if (!doctor.getPatients().contains(patient)) {
            doctor.getPatients().add(patient);
            personalRepository.save(doctor);
        }
    }

    @Override
    @Transactional
    public void removePatient(AssignAndRemoveRequest request, Long userId, String role) {
        var doctor = getPersonalPatientsOrThrowById(request.doctorId());
        var patient = getPatientOrThrowById(request.patientId());
        verifyDoctorPermission(role, doctor.getId(), userId);
        if (doctor.getPatients().contains(patient)) {
            doctor.getPatients().remove(patient);
            personalRepository.save(doctor);
        }
    }

    @Override
    public List<PatientResponse> getPatientsOfDoctor(Long doctorId) {
        var doctor = getPersonalPatientsOrThrowById(doctorId);
        return patientMapper.toResponseList(doctor.getPatients());
    }

    private Personal getPersonalOrThrowById(Long personalId) {
        return personalRepository.findById(personalId)
                .orElseThrow(() -> new ResourceNotFoundException("Personal not fount with id: " + personalId));
    }

    private Personal getPersonalPatientsOrThrowById(Long personalId) {
        return personalRepository.findDoctorPatientsById(personalId)
                .orElseThrow(() -> new ResourceNotFoundException("Personal not fount with id: " + personalId));
    }

    private Specialty getSpecialtyOrThrowById(Long specialtyId) {
        if(specialtyId == null){
            return null;
        }
        return  specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ResourceNotFoundException("Specialty not found with id: " + specialtyId));
    }

    private Role getRoleOrThrowById(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));
    }

    private Patient getPatientOrThrowById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
    }

    public void verifyDoctorPermission(String role, Long accountId, Long userId) {
        if (role.equals(ERole.DOCTOR.name()) && !accountId.equals(userId))
            throw new ForbiddenException("Not authorize to do this");
    }
}
