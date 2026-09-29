package com.example.scheduler.service.impl;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.dto.clinic.ClinicCreatedResponse;
import com.example.scheduler.dto.clinic.ClinicRequest;
import com.example.scheduler.dto.clinic.ClinicResponse;
import com.example.scheduler.entity.Account;
import com.example.scheduler.entity.Personal;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.BadRequestException;
import com.example.scheduler.mapper.ClinicMapper;
import com.example.scheduler.repository.AccountRepository;
import com.example.scheduler.repository.ClinicRepository;
import com.example.scheduler.repository.PersonalRepository;
import com.example.scheduler.repository.RoleRepository;
import com.example.scheduler.service.ClinicService;
import com.example.scheduler.service.SchemaProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicServiceImpl implements ClinicService {

    private final ClinicRepository clinicRepository;
    private final ClinicMapper clinicMapper;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final PersonalRepository personalRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SchemaProvisioningService schemaProvisioningService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public ClinicCreatedResponse createClinic(ClinicRequest request) {
        var tx = new TransactionTemplate(transactionManager);
        var clinic = tx.execute(_ -> clinicRepository.save(clinicMapper.toEntity(request)));
        var schemaName = "clinic_" + clinic.getId();
        schemaProvisioningService.createTenantSchema(schemaName);
        try {
            TenantContext.setCurrentTenant(schemaName);
            return tx.execute(_ -> {
                var adminRole = roleRepository.getByName(ERole.ADMINISTRATOR);
                if(accountRepository.existsByEmailOrCi(request.adminEmail(), request.adminCi()))
                    throw new BadRequestException("Account with email " + request.adminEmail() + " or ci"+ request.adminCi() +" already exists");
                var account = accountRepository.save(Account.builder()
                        .name(request.adminName())
                        .email(request.adminEmail())
                        .ci(request.adminCi())
                        .password(passwordEncoder.encode(request.adminPassword()))
                        .build());
                var admin = personalRepository.save(Personal.builder()
                        .account(account)
                        .role(adminRole)
                        .build());
                return new ClinicCreatedResponse(
                        clinic.getId(),
                        clinic.getName(),
                        clinic.getPhoneNumber(),
                        admin.getId(),
                        account.getEmail());
            });
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public List<ClinicResponse> findAllClinics() {
        return clinicMapper.toResponseList(clinicRepository.findAll());
    }
}
