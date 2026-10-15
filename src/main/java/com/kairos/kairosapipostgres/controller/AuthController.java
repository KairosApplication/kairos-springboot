package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.dto.response.AuthResponse;
import com.kairos.kairosapipostgres.dto.response.CsrfResponse;
import com.kairos.kairosapipostgres.dto.response.TokenResponse;
import com.kairos.kairosapipostgres.service.TokenAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final TokenAuthService tokens;

    public AuthController(TokenAuthService tokens) {
        this.tokens = tokens;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestParam String email, @RequestParam String password,
                                               HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(tokens.login(email, password, request, response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(tokens.refresh(request, response));
    }

    @GetMapping("/login")
    public ResponseEntity<CsrfResponse> login(CsrfToken csrfToken) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(new CsrfResponse(
                csrfToken.getHeaderName(), csrfToken.getToken()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        return ResponseEntity.status(HttpStatus.OK).body(new AuthResponse(
                authentication.getName(),
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .map(authority -> authority.substring(5))
                        .sorted()
                        .toList()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        tokens.logout(request, response, authentication);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
