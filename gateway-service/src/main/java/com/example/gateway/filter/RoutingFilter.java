package com.example.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class RoutingFilter implements WebFilter, Ordered {

    private final WebClient webClient;
    private final String providerUrl;
    private final String scheduleUrl;
    private final String appointmentUrl;

    public RoutingFilter(
            WebClient webClient,
            @Value("${PROVIDER_SERVICE_URL:http://localhost:8081}") String providerUrl,
            @Value("${SCHEDULE_SERVICE_URL:http://localhost:8082}") String scheduleUrl,
            @Value("${APPOINTMENT_SERVICE_URL:http://localhost:8083}") String appointmentUrl) {
        this.webClient = webClient;
        this.providerUrl = providerUrl;
        this.scheduleUrl = scheduleUrl;
        this.appointmentUrl = appointmentUrl;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String targetBase = resolveTarget(path);
        if (targetBase == null) {
            return chain.filter(exchange);
        }
        return forward(exchange, targetBase);
    }

    private String resolveTarget(String path) {
        // Schedule routes checked first — more specific than /api/providers/**
        if (path.matches("/api/providers/[^/]+/schedules(/.*)?")) {
            return scheduleUrl;
        }
        if (path.startsWith("/api/providers")) {
            return providerUrl;
        }
        if (path.startsWith("/api/appointments")) {
            return appointmentUrl;
        }
        return null;
    }

    private Mono<Void> forward(ServerWebExchange exchange, String targetBase) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        URI targetUri = UriComponentsBuilder.fromUriString(targetBase)
                .replacePath(request.getPath().value())
                .replaceQuery(request.getURI().getQuery())
                .build(true)
                .toUri();

        return webClient.method(request.getMethod())
                .uri(targetUri)
                .headers(h -> {
                    h.addAll(request.getHeaders());
                    h.remove(HttpHeaders.HOST);
                })
                .body(request.getBody(), DataBuffer.class)
                .exchangeToMono(clientResponse -> {
                    response.setStatusCode(clientResponse.statusCode());
                    clientResponse.headers().asHttpHeaders().forEach((name, values) -> {
                        // Let the container set these based on actual response body
                        if (!name.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING)
                                && !name.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
                            response.getHeaders().addAll(name, values);
                        }
                    });
                    return response.writeWith(clientResponse.bodyToFlux(DataBuffer.class));
                });
    }
}
