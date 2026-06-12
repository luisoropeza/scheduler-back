package com.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Value("${PROVIDER_SERVICE_URL:http://localhost:8081}")
    private String providerServiceUrl;

    @Value("${SCHEDULE_SERVICE_URL:http://localhost:8082}")
    private String scheduleServiceUrl;

    @Value("${APPOINTMENT_SERVICE_URL:http://localhost:8083}")
    private String appointmentServiceUrl;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Schedule routes must be declared before provider routes — more specific path wins.
                .route("schedule-service", r -> r
                        .path("/api/providers/*/schedules", "/api/providers/*/schedules/**")
                        .uri(scheduleServiceUrl))
                .route("provider-service", r -> r
                        .path("/api/providers", "/api/providers/**")
                        .uri(providerServiceUrl))
                .route("appointment-service", r -> r
                        .path("/api/appointments", "/api/appointments/**")
                        .uri(appointmentServiceUrl))
                .build();
    }
}
