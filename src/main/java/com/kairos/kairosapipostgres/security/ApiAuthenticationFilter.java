package com.kairos.kairosapipostgres.security;

import com.kairos.kairosapipostgres.service.TokenAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;

/** Authentication runs after CSRF validation, before authorization and controller dispatch. */
public class ApiAuthenticationFilter extends OncePerRequestFilter {
    private final JwtDecoder decoder;
    private final JwtDecoder proofDecoder;
    private final JwtAuthenticationConverter converter;
    private final RedisRefreshTokenStore sessions;
    private final TokenAuthService tokens;
    private final AuthCookies cookies;
    private final Clock clock;
    private final DefaultBearerTokenResolver bearer = new DefaultBearerTokenResolver();

    public ApiAuthenticationFilter(JwtDecoder decoder, JwtDecoder proofDecoder, JwtAuthenticationConverter converter,
                                    RedisRefreshTokenStore sessions, TokenAuthService tokens, AuthCookies cookies, Clock clock) {
        this.decoder = decoder;
        this.proofDecoder = proofDecoder;
        this.converter = converter;
        this.sessions = sessions;
        this.tokens = tokens;
        this.cookies = cookies;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !path(request).startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            authenticate(request, response);
        } catch (JwtException | AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(401);
            response.setHeader("Cache-Control", "no-store");
            return;
        } catch (DataAccessException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(503);
            response.setHeader("Cache-Control", "no-store");
            return;
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, HttpServletResponse response) {
        String path = path(request);
        boolean logout = path.equals("/api/v1/auth/logout");
        // Refresh credentials authorize these public endpoints independently of an access cookie.
        if (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/refresh")) {
            return;
        }
        String header = bearer.resolve(request);
        String value = header != null ? header : cookies.accessValue(request);
        Jwt jwt = null;
        if (value != null) {
            try {
                jwt = decoder.decode(value);
            } catch (JwtException exception) {
                // This decoder still verifies signature, algorithm, issuer, audience and all required claims.
                Jwt proof = proofDecoder.decode(value);
                if (!expired(proof)) {
                    throw exception;
                }
                if (logout) {
                    jwt = proof;
                } else {
                    jwt = renew(request, response, proof, header != null);
                }
            }
        } else if (!logout && cookies.refreshValue(request) != null
                && !path.equals("/api/v1/users/registration")) {
            jwt = decoder.decode(tokens.automaticRefresh(request, response, null).accessToken());
        }
        if (jwt != null) {
            if (!logout) {
                sessions.assertActive(jwt.getClaimAsString("sid"), jwt.getSubject());
            }
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(converter.convert(jwt));
            SecurityContextHolder.setContext(context);
        } else if (header != null) {
            throw new BadCredentialsException("Invalid access token");
        }
    }

    private String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private Jwt renew(HttpServletRequest request, HttpServletResponse response, Jwt expired, boolean fromHeader) {
        String cookie = cookies.accessValue(request);
        if (fromHeader && cookie != null) {
            Jwt current = proofDecoder.decode(cookie);
            if (!current.getClaimAsString("sid").equals(expired.getClaimAsString("sid"))) {
                throw new BadCredentialsException("Access sessions differ");
            }
            if (!expired(current)) {
                return decoder.decode(cookie);
            }
        }
        return decoder.decode(tokens.automaticRefresh(request, response, expired).accessToken());
    }

    private boolean expired(Jwt jwt) {
        var expires = jwt.getExpiresAt();
        if (expires == null) {
            throw new BadCredentialsException("Missing access expiration");
        }
        return !expires.isAfter(clock.instant());
    }
}
