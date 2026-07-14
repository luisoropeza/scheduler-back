package com.example.user.service.impl;

import com.example.user.dto.PatientRequest;
import com.example.user.dto.PatientResponse;
import com.example.user.dto.PersonalResponse;
import com.example.user.entity.Patient;
import com.example.user.exception.ForbiddenException;
import com.example.user.exception.ResourceNotFoundException;
import com.example.user.mapper.PatientMapper;
import com.example.user.mapper.PersonalMapper;
import com.example.user.repository.PatientRepository;
import com.example.user.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final PersonalMapper personalMapper;

    public Page<PatientResponse> findAll(Pageable pageable) {
        return patientRepository.findAll(pageable).map(patientMapper::toResponse);
    }

    public PatientResponse findById(Long id) {
        return patientMapper.toResponse(getOrThrow(id));
    }

    public PatientResponse findByPhoneNumber(String phoneNumber) {
        return patientMapper.toResponse(patientRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with phone number: " + phoneNumber)));
    }

    @Transactional
    public PatientResponse update(Long id, PatientRequest request, Long userId) {
        if (!id.equals(userId))
            throw new ForbiddenException("This user does not authorize to update this user");
        Patient patient = getOrThrow(id);
        patientMapper.toEntityUpdated(request, patient);
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Transactional
    public void deactivate(Long id) {
        Patient patient = getOrThrow(id);
        patient.setActive(false);
        patientRepository.save(patient);
    }

    public List<PersonalResponse> getDoctorsOfPatient(Long patientId) {
        Patient patient = getOrThrow(patientId);
        return personalMapper.toResponseList(patient.getDoctors());
    }

    private Patient getOrThrow(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
    }
}
