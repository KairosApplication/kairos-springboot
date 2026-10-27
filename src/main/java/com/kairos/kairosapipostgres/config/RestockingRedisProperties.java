package com.kairos.kairosapipostgres.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("app.restocking-redis")
public record RestockingRedisProperties(boolean enabled, URI url, String internalKey,
                                         Duration connectTimeout, Duration readTimeout) {
    public RestockingRedisProperties {
        if (url == null || url.getHost() == null || url.getUserInfo() != null || url.getQuery() != null
                || url.getFragment() != null || !("http".equals(url.getScheme()) || "https".equals(url.getScheme()))) {
            throw new IllegalArgumentException("Configure a valid KAIROS_REDIS_API_URL without credentials");
        }
        if (enabled && (internalKey == null || internalKey.length() < 32)) {
            throw new IllegalArgumentException("KAIROS_INTERNAL_API_KEY must contain at least 32 characters");
        }
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()
                || readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException("Redis API timeouts must be positive");
        }
    }

    @Override
    public String toString() {
        return "RestockingRedisProperties[enabled=" + enabled + ", internalKey=REDACTED]";
    }
}
