package com.example.gateway.filter;

import com.example.gateway.config.GatewayProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
public class RoutingFilter implements WebFilter {

    private static final Set<String> EXCLUDED_REQUEST_HEADERS = Set.of("host", "content-length");
    private static final Set<String> EXCLUDED_RESPONSE_HEADERS = Set.of("transfer-encoding", "connection");

    private final WebClient webClient;
    private final GatewayProperties properties;

    public RoutingFilter(WebClient.Builder webClientBuilder, GatewayProperties properties) {
        this.webClient = webClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String targetBaseUrl = resolveTargetUrl(path);
        if (targetBaseUrl == null) {
            return chain.filter(exchange);
        }
        return proxy(exchange, targetBaseUrl);
    }

    private String resolveTargetUrl(String path) {
        // Schedule paths share the /api/providers/ prefix — must be checked first
        if (path.startsWith("/api/providers") && path.contains("/schedules")) return properties.getScheduleServiceUrl();
        if (path.startsWith("/api/providers")) return properties.getProviderServiceUrl();
        if (path.startsWith("/api/appointments")) return properties.getAppointmentServiceUrl();
        return null;
    }

    private Mono<Void> proxy(ServerWebExchange exchange, String targetBaseUrl) {
        ServerHttpRequest request = exchange.getRequest();
        String rawPath = request.getURI().getRawPath();
        String rawQuery = request.getURI().getRawQuery();
        String targetUri = targetBaseUrl + rawPath + (rawQuery != null ? "?" + rawQuery : "");

        return webClient
            .method(request.getMethod())
            .uri(targetUri)
            .headers(headers -> request.getHeaders().forEach((name, values) -> {
                if (!EXCLUDED_REQUEST_HEADERS.contains(name.toLowerCase())) {
                    headers.addAll(name, values);
                }
            }))
            .body(request.getBody(), DataBuffer.class)
            .exchangeToMono(clientResponse -> {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(clientResponse.statusCode());
                clientResponse.headers().asHttpHeaders().forEach((name, values) -> {
                    if (!EXCLUDED_RESPONSE_HEADERS.contains(name.toLowerCase())) {
                        response.getHeaders().addAll(name, values);
                    }
                });
                return response.writeWith(clientResponse.bodyToFlux(DataBuffer.class));
            });
    }
}
