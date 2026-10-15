package com.kairos.kairosapipostgres.security;

import org.springframework.security.authentication.BadCredentialsException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/** Opaque to clients. The random locator is never sufficient to authorize a mutation. */
public final class RefreshToken {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final String sessionId;
    private final String value;

    private RefreshToken(String sessionId, String value) {
        this.sessionId = sessionId;
        this.value = value;
    }

    public static RefreshToken create() {
        return rotate(UUID.randomUUID().toString().replace("-", ""));
    }

    public static RefreshToken rotate(String sessionId) {
        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        return new RefreshToken(sessionId, sessionId + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(secret));
    }

    public static RefreshToken parse(String value) {
        if (value == null || !value.matches("[a-f0-9]{32}\\.[A-Za-z0-9_-]{43}")) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        return new RefreshToken(value.substring(0, 32), value);
    }

    public String sessionId() {
        return sessionId;
    }

    public String value() {
        return value;
    }

    public String hash() {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
