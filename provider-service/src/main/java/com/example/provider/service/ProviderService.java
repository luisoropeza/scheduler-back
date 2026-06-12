package com.example.provider.service;

import com.example.provider.dto.ProviderRequest;
import com.example.provider.dto.ProviderResponse;

import java.util.List;

public interface ProviderService {
    List<ProviderResponse> findAll(String specialty);
    ProviderResponse findById(Long id);
    ProviderResponse create(ProviderRequest request);
    ProviderResponse update(Long id, ProviderRequest request);
    void deactivate(Long id);
}
