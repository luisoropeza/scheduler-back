package com.example.user.service;

import com.example.user.dto.PatientResponse;
import com.example.user.dto.PersonalRequest;
import com.example.user.dto.PersonalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PersonalService {
    Page<PersonalResponse> findAll(Long specialtyId, Boolean isActive, Pageable pageable);
    PersonalResponse findById(Long id);
    PersonalResponse update(Long id, PersonalRequest request);
    void deactivate(Long id);
    void assignPatient(Long doctorId, Long patientId);
    void removePatient(Long doctorId, Long patientId);
    List<PatientResponse> getPatientsOfDoctor(Long doctorId);
}
