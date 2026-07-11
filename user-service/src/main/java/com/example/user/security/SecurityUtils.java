package com.example.user.security;

import org.springframework.security.core.Authentication;

import java.util.Objects;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static String extractRole(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(a -> Objects.requireNonNull(a.getAuthority()).replace("ROLE_", ""))
                .findFirst()
                .orElse(null);
    }
}
