package com.example.user.service;

import com.example.user.dto.LoginRequest;
import com.example.user.dto.LoginResponse;
import com.example.user.dto.PatientRegisterRequest;
import com.example.user.dto.PersonalRegisterRequest;

public interface AuthService {
    LoginResponse registerPatient(PatientRegisterRequest request);
    LoginResponse loginPatient(LoginRequest request);
    LoginResponse registerPersonal(PersonalRegisterRequest request);
    LoginResponse loginPersonal(LoginRequest request);
}
