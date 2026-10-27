package com.kairos.kairosapipostgres.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.auth")
public record AuthProperties(String signingKey, String issuer, String audience,
                             Duration accessTtl, Duration sessionTtl, boolean cookieSecure,
                             String cookieSameSite) {
    public AuthProperties {
        if (signingKey == null || signingKey.isBlank() || issuer == null || issuer.isBlank()
                || audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("Configure AUTH_SIGNING_KEY, AUTH_ISSUER and AUTH_AUDIENCE");
        }
        if (accessTtl == null || accessTtl.toSeconds() < 1 || sessionTtl == null
                || sessionTtl.toSeconds() < 1) {
            throw new IllegalArgumentException("Authentication durations must be positive seconds");
        }
        if (!"Lax".equals(cookieSameSite) && !"Strict".equals(cookieSameSite)
                && !"None".equals(cookieSameSite)) {
            throw new IllegalArgumentException("Cookie SameSite must be Lax, Strict or None");
        }
        if ("None".equals(cookieSameSite) && !cookieSecure) {
            throw new IllegalArgumentException("SameSite=None requires Secure cookies");
        }
    }
}
