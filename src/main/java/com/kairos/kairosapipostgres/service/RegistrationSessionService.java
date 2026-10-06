package com.kairos.kairosapipostgres.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Service;

@Service
public class RegistrationSessionService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final CsrfAuthenticationStrategy csrfStrategy;
    private final ChangeSessionIdAuthenticationStrategy sessionStrategy = new ChangeSessionIdAuthenticationStrategy();

    public RegistrationSessionService(AuthenticationManager authenticationManager,
                                      SecurityContextRepository contextRepository,
                                      CsrfTokenRepository csrfTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.csrfStrategy = new CsrfAuthenticationStrategy(csrfTokenRepository);
    }

    public void login(String email, String password, HttpServletRequest request, HttpServletResponse response) {
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        sessionStrategy.onAuthentication(authentication, request, response);
        csrfStrategy.onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
