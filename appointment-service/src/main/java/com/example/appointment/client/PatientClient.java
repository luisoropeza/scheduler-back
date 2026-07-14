package com.example.appointment.client;

import com.example.appointment.dto.PatientResponse;
import com.example.appointment.exception.BusinessException;
import com.example.appointment.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PatientClient {

    private final RestClient restClient;

    public PatientClient(@Value("${services.user-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public PatientResponse findByPhoneNumber(String phoneNumber) {
        try {
            return restClient.get()
                    .uri("/internal/patients/lookup?phoneNumber={phoneNumber}", phoneNumber)
                    .retrieve()
                    .body(PatientResponse.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Patient not found with phone number: " + phoneNumber);
            }
            throw new BusinessException("User service error: " + e.getStatusCode());
        } catch (RestClientException e) {
            throw new BusinessException("Cannot reach user-service: " + e.getMessage());
        }
    }
}
