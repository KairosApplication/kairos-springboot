package com.kairos.kairosapipostgres.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class SessionCookieRenewalFilter extends OncePerRequestFilter {

    private final CookieSerializer cookieSerializer;

    public SessionCookieRenewalFilter(CookieSerializer cookieSerializer) {
        this.cookieSerializer = cookieSerializer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "POST".equals(request.getMethod())
                && (request.getContextPath() + "/api/v1/auth/logout").equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var session = request.getSession(false);
        if (session != null && authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, session.getId()));
        }
        filterChain.doFilter(request, response);
    }
}
