package com.kairos.kairosapipostgres.security;

import com.kairos.kairosapipostgres.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class AuthCookies {
    public static final String ACCESS = "KAIROS_ACCESS";
    public static final String REFRESH = "KAIROS_REFRESH";
    public static final String CSRF = "KAIROS_CSRF";
    private final AuthProperties properties;
    private final Clock clock;

    public AuthCookies(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public String refreshValue(HttpServletRequest request) {
        return value(request, REFRESH);
    }

    public String accessValue(HttpServletRequest request) {
        return value(request, ACCESS);
    }

    private String value(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void write(HttpServletResponse response, RefreshToken token, Instant expires) {
        Duration remaining = Duration.between(clock.instant(), expires);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH, "/api/v1", token.value(),
                remaining.isNegative() ? Duration.ZERO : remaining).toString());
    }

    public void writeAccess(HttpServletResponse response, String access) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(ACCESS, "/api/v1", access, properties.accessTtl()).toString());
        // Remove cookies issued before automatic renewal was introduced.
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH, "/api/v1/auth", "", Duration.ZERO).toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(REFRESH, "/api/v1", "", Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS, "/api/v1", "", Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH, "/api/v1/auth", "", Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String name, String path, String value, Duration maxAge) {
        return ResponseCookie.from(name, value).path(path).httpOnly(true)
                .secure(properties.cookieSecure()).sameSite(properties.cookieSameSite()).maxAge(maxAge).build();
    }
}
