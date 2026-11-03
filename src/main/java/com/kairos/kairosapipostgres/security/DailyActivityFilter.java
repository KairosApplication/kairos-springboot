package com.kairos.kairosapipostgres.security;

import com.kairos.kairosapipostgres.service.DailyActivityService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/** Records successful, authenticated human-facing API activity, never login attempts/probes. */
public class DailyActivityFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(DailyActivityFilter.class);
    private final DailyActivityService activity;
    private final Counter successes;
    private final Counter failures;

    public DailyActivityFilter(DailyActivityService activity, MeterRegistry meters) {
        this.activity = activity;
        this.successes = meters.counter("kairos.dau.record.success");
        this.failures = meters.counter("kairos.dau.record.failures");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String agent = request.getHeader("User-Agent");
        String normalized = agent == null ? "" : agent.toLowerCase(Locale.ROOT);
        return !path.startsWith("/api/v1/")
                || (path.startsWith("/api/v1/auth/") && !path.equals("/api/v1/auth/me"))
                || path.equals("/api/v1/users/registration")
                || normalized.contains("bot") || normalized.contains("crawler")
                || normalized.contains("spider") || normalized.contains("probe");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        chain.doFilter(request, response);
        if (response.getStatus() >= 400 || !(authentication instanceof JwtAuthenticationToken jwt)
                || !authentication.isAuthenticated()) {
            return;
        }
        try {
            activity.record(Long.parseLong(jwt.getToken().getSubject()));
            successes.increment();
        } catch (RuntimeException failure) {
            // Fail open for metrics only: never log tokens, request parameters or personal data.
            failures.increment();
            LOG.warn("DAU recording failed; authenticated response preserved (type={})",
                    failure.getClass().getSimpleName());
        }
    }
}
