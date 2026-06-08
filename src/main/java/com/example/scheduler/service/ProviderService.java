package com.example.scheduler.service;

import com.example.scheduler.dto.provider.ProviderRequest;
import com.example.scheduler.dto.provider.ProviderResponse;

import java.util.List;

public interface ProviderService {

    List<ProviderResponse> findAll(String specialty);

    ProviderResponse findById(Long id);

    ProviderResponse create(ProviderRequest request);

    ProviderResponse update(Long id, ProviderRequest request);

    void deactivate(Long id);
}
