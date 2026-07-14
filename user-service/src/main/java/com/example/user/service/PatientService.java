package com.example.user.service;

import com.example.user.dto.PatientRequest;
import com.example.user.dto.PatientResponse;
import com.example.user.dto.PersonalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PatientService {
    Page<PatientResponse> findAll(Pageable pageable);
    PatientResponse findById(Long id);
    PatientResponse findByPhoneNumber(String phoneNumber);
    PatientResponse update(Long id, PatientRequest request, Long userId);
    void deactivate(Long id);
    List<PersonalResponse> getDoctorsOfPatient(Long patientId);
}
