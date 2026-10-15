package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.config.AuthProperties;
import com.kairos.kairosapipostgres.dto.response.TokenResponse;
import com.kairos.kairosapipostgres.repository.UserRepository;
import com.kairos.kairosapipostgres.security.AuthCookies;
import com.kairos.kairosapipostgres.security.RedisRefreshTokenStore;
import com.kairos.kairosapipostgres.security.RefreshToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;

@Service
public class TokenAuthService {
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetails;
    private final UserRepository users;
    private final RedisRefreshTokenStore sessions;
    private final JwtEncoder encoder;
    private final AuthProperties properties;
    private final Clock clock;
    private final AuthCookies cookies;
    private final CsrfTokenRepository csrf;
    private final SecretKey signingKey;

    public TokenAuthService(AuthenticationManager authenticationManager, CustomUserDetailsService userDetails,
                            UserRepository users, RedisRefreshTokenStore sessions, JwtEncoder encoder,
                            AuthProperties properties, Clock clock, AuthCookies cookies, CsrfTokenRepository csrf,
                            SecretKey signingKey) {
        this.authenticationManager = authenticationManager;
        this.userDetails = userDetails;
        this.users = users;
        this.sessions = sessions;
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
        this.cookies = cookies;
        this.csrf = csrf;
        this.signingKey = signingKey;
    }

    public TokenResponse login(String email, String password, HttpServletRequest request,
                                HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        var user = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        RefreshToken refresh = RefreshToken.create();
        TokenResponse access = issue(user.getId(), user.getEmail(), refresh.sessionId(), authentication.getAuthorities());
        var expires = sessions.create(user.getId(), refresh, properties.sessionTtl());
        cookies.write(response, refresh, expires);
        cookies.writeAccess(response, access.accessToken());
        csrf.saveToken(null, request, response);
        return access;
    }

    public TokenResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        return refresh(request, response, false);
    }

    public TokenResponse automaticRefresh(HttpServletRequest request, HttpServletResponse response, Jwt expired) {
        RefreshToken previous = RefreshToken.parse(cookies.refreshValue(request));
        if (expired != null && !previous.sessionId().equals(expired.getClaimAsString("sid"))) {
            throw new BadCredentialsException("Access and refresh sessions differ");
        }
        return refresh(request, response, true);
    }

    private TokenResponse refresh(HttpServletRequest request, HttpServletResponse response, boolean automatic) {
        RefreshToken previous = RefreshToken.parse(cookies.refreshValue(request));
        RefreshToken next = automatic ? previous.automaticSuccessor(signingKey) : RefreshToken.rotate(previous.sessionId());
        var session = automatic ? sessions.automatic(previous, next, false) : sessions.inspect(previous);
        UserDetails current;
        try {
            var user = users.findById(session.userId())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            current = userDetails.loadUserByUsername(user.getEmail());
        } catch (UsernameNotFoundException exception) {
            sessions.revoke(previous);
            throw new BadCredentialsException("User cannot authenticate");
        }
        if (!current.isEnabled() || !current.isAccountNonLocked()
                || !current.isAccountNonExpired() || !current.isCredentialsNonExpired()) {
            sessions.revoke(previous);
            throw new BadCredentialsException("User cannot authenticate");
        }
        TokenResponse access = issue(session.userId(), current.getUsername(), next.sessionId(), current.getAuthorities());
        var rotated = automatic ? sessions.automatic(previous, next, true) : sessions.rotate(previous, next);
        cookies.write(response, next, rotated.expiresAt());
        cookies.writeAccess(response, access.accessToken());
        return access;
    }

    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String refresh = cookies.refreshValue(request);
        if (authentication instanceof JwtAuthenticationToken jwt) {
            // A verified JWT identifies the current session. Never revoke a second device
            // merely because a stale/different refresh cookie accompanies the request.
            sessions.revokeAuthenticatedSession(jwt.getToken().getClaimAsString("sid"));
        } else if (refresh != null) {
            try {
                sessions.revoke(RefreshToken.parse(refresh));
            } catch (BadCredentialsException ignored) {
                // Malformed cookies are cleared, but never authorize revocation.
            }
        }
        cookies.clear(response);
        csrf.saveToken(null, request, response);
    }

    private TokenResponse issue(long userId, String email, String sessionId,
                                 Collection<? extends GrantedAuthority> authorities) {
        var now = clock.instant();
        var claims = JwtClaimsSet.builder().issuer(properties.issuer()).audience(List.of(properties.audience()))
                .subject(Long.toString(userId)).issuedAt(now).expiresAt(now.plus(properties.accessTtl()))
                .id(UUID.randomUUID().toString()).claim("sid", sessionId).claim("email", email)
                .claim("token_use", "access")
                .claim("roles", authorities.stream().map(GrantedAuthority::getAuthority)
                        .filter(role -> role.startsWith("ROLE_")).map(role -> role.substring(5)).toList()).build();
        String value = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenResponse(value, "Bearer", properties.accessTtl().toSeconds());
    }
}
