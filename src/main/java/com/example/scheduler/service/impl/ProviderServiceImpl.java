package com.example.scheduler.service.impl;

import com.example.scheduler.dto.provider.ProviderRequest;
import com.example.scheduler.dto.provider.ProviderResponse;
import com.example.scheduler.entity.Provider;
import com.example.scheduler.exception.ResourceNotFoundException;
import com.example.scheduler.mapper.ProviderMapper;
import com.example.scheduler.repository.ProviderRepository;
import com.example.scheduler.service.ProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProviderServiceImpl implements ProviderService {

    private final ProviderRepository providerRepository;
    private final ProviderMapper providerMapper;

    @Override
    public List<ProviderResponse> findAll(String specialty) {
        List<Provider> providers = StringUtils.hasText(specialty)
                ? providerRepository.findBySpecialtyIgnoreCaseAndActiveTrue(specialty)
                : providerRepository.findByActiveTrue();

        return providers.stream()
                .map(providerMapper::toResponse)
                .toList();
    }

    @Override
    public ProviderResponse findById(Long id) {
        return providerMapper.toResponse(getProviderOrThrow(id));
    }

    @Override
    @Transactional
    public ProviderResponse create(ProviderRequest request) {
        Provider provider = providerMapper.toEntity(request);
        return providerMapper.toResponse(providerRepository.save(provider));
    }

    @Override
    @Transactional
    public ProviderResponse update(Long id, ProviderRequest request) {
        Provider provider = getProviderOrThrow(id);
        providerMapper.updateEntity(request, provider);
        return providerMapper.toResponse(providerRepository.save(provider));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Provider provider = getProviderOrThrow(id);
        provider.setActive(false);
        providerRepository.save(provider);
    }

    private Provider getProviderOrThrow(Long id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + id));
    }
}
