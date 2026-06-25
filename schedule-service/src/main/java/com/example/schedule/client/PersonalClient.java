package com.example.schedule.client;

import com.example.schedule.dto.PersonalResponse;
import com.example.schedule.exception.BusinessException;
import com.example.schedule.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PersonalClient {
    private final RestClient restClient;

    public PersonalClient(@Value("${services.personal-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public PersonalResponse findById(Long id) {
        try {
            return restClient.get().uri("/api/personal/{id}", id).retrieve().body(PersonalResponse.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) throw new ResourceNotFoundException("Personal not found: " + id);
            throw new BusinessException("Personal service error: " + e.getStatusCode());
        } catch (RestClientException e) {
            throw new BusinessException("Cannot reach personal-service: " + e.getMessage());
        }
    }
}
