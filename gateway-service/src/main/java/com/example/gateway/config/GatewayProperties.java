package com.example.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway")
@Data
public class GatewayProperties {

    private String providerServiceUrl;
    private String scheduleServiceUrl;
    private String appointmentServiceUrl;
}
