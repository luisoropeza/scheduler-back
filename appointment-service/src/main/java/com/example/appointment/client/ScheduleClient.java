package com.example.appointment.client;

import com.example.appointment.dto.ScheduleResponse;
import com.example.appointment.exception.BusinessException;
import com.example.appointment.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ScheduleClient {

    private final RestClient restClient;

    public ScheduleClient(@Value("${services.schedule-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public ScheduleResponse findById(Long scheduleId) {
        try {
            return restClient.get()
                    .uri("/internal/schedules/{id}", scheduleId)
                    .retrieve()
                    .body(ScheduleResponse.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Schedule not found with id: " + scheduleId);
            }
            throw new BusinessException("Schedule service error: " + e.getStatusCode());
        } catch (RestClientException e) {
            throw new BusinessException("Cannot reach schedule-service: " + e.getMessage());
        }
    }

    public void bookSchedule(Long scheduleId) {
        try {
            restClient.patch()
                    .uri("/internal/schedules/{id}/book", scheduleId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw new BusinessException("Failed to book schedule: " + e.getMessage());
        } catch (RestClientException e) {
            throw new BusinessException("Cannot reach schedule-service: " + e.getMessage());
        }
    }

    public void releaseSchedule(Long scheduleId) {
        try {
            restClient.patch()
                    .uri("/internal/schedules/{id}/release", scheduleId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw new BusinessException("Failed to release schedule: " + e.getMessage());
        } catch (RestClientException e) {
            throw new BusinessException("Cannot reach schedule-service: " + e.getMessage());
        }
    }
}
