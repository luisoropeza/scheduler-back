package com.example.provider.config;

import com.example.provider.entity.Provider;
import com.example.provider.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final ProviderRepository providerRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (providerRepository.count() > 0) return;

        providerRepository.save(Provider.builder()
                .name("Dr. Ana García").specialty("General Medicine")
                .phone("+1-555-0101").email("ana.garcia@clinic.com").build());

        providerRepository.save(Provider.builder()
                .name("Dr. Carlos Méndez").specialty("Dentistry")
                .phone("+1-555-0202").email("carlos.mendez@clinic.com").build());

        providerRepository.save(Provider.builder()
                .name("Dr. Laura Torres").specialty("Pediatrics")
                .phone("+1-555-0303").email("laura.torres@clinic.com").build());
    }
}
