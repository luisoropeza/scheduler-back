package com.example.scheduler.service.impl;

import com.example.scheduler.dto.patient.PatientRegisterRequest;
import com.example.scheduler.dto.patient.PatientRequest;
import com.example.scheduler.dto.patient.PatientResponse;
import com.example.scheduler.dto.personal.PersonalResponse;
import com.example.scheduler.entity.Patient;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.PatientMapper;
import com.example.scheduler.mapper.PersonalMapper;
import com.example.scheduler.repository.AccountRepository;
import com.example.scheduler.repository.PatientRepository;
import com.example.scheduler.repository.RoleRepository;
import com.example.scheduler.service.PatientService;
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
public class PatientServiceImpl implements PatientService {
    private final PatientRepository patientRepository;
    private final RoleRepository  roleRepository;
    private final AccountRepository accountRepository;
    private final PatientMapper patientMapper;
    private final PersonalMapper personalMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public Page<PatientResponse> findAllPatients(Pageable pageable) {
        return patientRepository.findAll(pageable).map(patientMapper::toResponse);
    }

    @Override
    @Transactional
    public PatientResponse createPatient(PatientRegisterRequest request) {
        var patient = patientRepository.findByAccountCi(request.ci())
                .orElseGet(() -> {
                    if(accountRepository.existsByEmailOrCi(request.email(), request.ci()))
                        throw new BadRequestException("Account with email " + request.email() + " or ci"+ request.ci() +" already exists");
                    return patientMapper.toEntity(request);
                });
        var role = roleRepository.getByName(ERole.PATIENT);
        patient.setRole(role);
        patient.getAccount().setPassword(passwordEncoder.encode(request.password()));
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Override
    public PatientResponse findPatientById(Long patientId) {
        return patientMapper.toResponse(getPatientOrThrowById(patientId));
    }

    @Override
    public PatientResponse findPatientByPhoneNumber(String phoneNumber) {
        return patientMapper.toResponse(patientRepository.findByAccountPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with phone number: " + phoneNumber)));
    }

    @Override
    @Transactional
    public PatientResponse updatePatientById(Long patientId, PatientRequest request) {
        var patient = getPatientOrThrowById(patientId);
        patientMapper.toEntityUpdated(request, patient);
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Override
    @Transactional
    public void deactivatePatientById(Long patientId) {
        var patient = getPatientOrThrowById(patientId);
        patient.setActive(false);
        patientRepository.save(patient);
    }

    @Override
    public List<PersonalResponse> getDoctorsOfPatient(Long patientId) {
        var patient = getPatientDoctorsOrThrowById(patientId);
        return personalMapper.toResponseList(patient.getDoctors());
    }

    private Patient getPatientOrThrowById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
    }

    private Patient getPatientDoctorsOrThrowById(Long patientId) {
        return patientRepository.findPatientDoctorsById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));
    }
}
