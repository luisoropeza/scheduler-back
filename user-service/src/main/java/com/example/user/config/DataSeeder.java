package com.example.user.config;

import com.example.user.entity.Patient;
import com.example.user.entity.Personal;
import com.example.user.entity.Specialty;
import com.example.user.enums.ERole;
import com.example.user.repository.PatientRepository;
import com.example.user.repository.PersonalRepository;
import com.example.user.repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {
    private final SpecialtyRepository specialtyRepository;
    private final PersonalRepository personalRepository;
    private final PatientRepository patientRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (specialtyRepository.count() == 0) {
            specialtyRepository.save(Specialty.builder().name("General Medicine").build());
            specialtyRepository.save(Specialty.builder().name("Dentistry").build());
            specialtyRepository.save(Specialty.builder().name("Pediatrics").build());
        }
        if (personalRepository.count() == 0) {
            String pwd = passwordEncoder.encode("password123");
            Specialty gm = specialtyRepository.findByNameIgnoreCase("General Medicine").orElseThrow();
            Specialty dent = specialtyRepository.findByNameIgnoreCase("Dentistry").orElseThrow();
            Specialty peds = specialtyRepository.findByNameIgnoreCase("Pediatrics").orElseThrow();
            personalRepository.save(Personal.builder().name("Dr. Ana García").email("ana.garcia@clinic.com").password(pwd).role(ERole.DOCTOR).specialty(gm).build());
            personalRepository.save(Personal.builder().name("Dr. Carlos Méndez").email("carlos.mendez@clinic.com").password(pwd).role(ERole.DOCTOR).specialty(dent).build());
            personalRepository.save(Personal.builder().name("Dr. Laura Torres").email("laura.torres@clinic.com").password(pwd).role(ERole.DOCTOR).specialty(peds).build());
            personalRepository.save(Personal.builder().name("Maria Ramos").email("maria.ramos@clinic.com").password(pwd).role(ERole.RECEPTIONIST).build());
        }
        if (patientRepository.count() == 0) {
            String pwd = passwordEncoder.encode("password123");
            Patient john = patientRepository.save(Patient.builder().name("John Smith").email("john.smith@email.com").phoneNumber("+1-555-1001").password(pwd).build());
            Patient maria = patientRepository.save(Patient.builder().name("María López").email("maria.lopez@email.com").phoneNumber("+1-555-1002").password(pwd).build());
            Patient james = patientRepository.save(Patient.builder().name("James Wilson").email("james.wilson@email.com").phoneNumber("+1-555-1003").password(pwd).build());

            Personal ana = personalRepository.findByEmail("ana.garcia@clinic.com").orElseThrow();
            Personal carlos = personalRepository.findByEmail("carlos.mendez@clinic.com").orElseThrow();
            ana.getPatients().add(john);
            ana.getPatients().add(maria);
            carlos.getPatients().add(james);
            personalRepository.save(ana);
            personalRepository.save(carlos);
        }
    }
}
