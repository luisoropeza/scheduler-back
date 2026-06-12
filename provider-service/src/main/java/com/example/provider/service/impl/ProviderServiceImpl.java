package com.example.provider.service.impl;

import com.example.provider.dto.ProviderRequest;
import com.example.provider.dto.ProviderResponse;
import com.example.provider.entity.Provider;
import com.example.provider.exception.ResourceNotFoundException;
import com.example.provider.mapper.ProviderMapper;
import com.example.provider.repository.ProviderRepository;
import com.example.provider.service.ProviderService;
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
        return providers.stream().map(providerMapper::toResponse).toList();
    }

    @Override
    public ProviderResponse findById(Long id) {
        return providerMapper.toResponse(getProviderOrThrow(id));
    }

    @Override
    @Transactional
    public ProviderResponse create(ProviderRequest request) {
        return providerMapper.toResponse(providerRepository.save(providerMapper.toEntity(request)));
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
