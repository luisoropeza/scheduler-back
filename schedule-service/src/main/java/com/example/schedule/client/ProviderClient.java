package com.example.schedule.client;

import com.example.schedule.dto.ProviderResponse;
import com.example.schedule.exception.BusinessException;
import com.example.schedule.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ProviderClient {

    private final RestClient restClient;

    public ProviderClient(@Value("${services.provider-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public ProviderResponse findById(Long providerId) {
        try {
            return restClient.get()
                    .uri("/api/providers/{id}", providerId)
                    .retrieve()
                    .body(ProviderResponse.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Provider not found with id: " + providerId);
            }
            throw new BusinessException("Provider service error: " + e.getStatusCode());
        } catch (RestClientException e) {
            throw new BusinessException("Cannot reach provider-service: " + e.getMessage());
        }
    }
}
