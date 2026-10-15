package com.kairos.kairosapipostgres.config;

import com.kairos.kairosapipostgres.security.AuthCookies;
import com.kairos.kairosapipostgres.security.RefreshToken;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

class JwtSecurityTest {
    private static final String KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private final Instant now = Instant.parse("2026-01-01T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final JwtConfig config = new JwtConfig();
    private final AuthProperties properties = properties(KEY, true, "Lax");
    private final JwtEncoder encoder = config.jwtEncoder(config.jwtSigningKey(properties));
    private final JwtDecoder decoder = config.jwtDecoder(config.jwtSigningKey(properties), properties, clock);

    @Test
    void validatesHs256AndRequiredClaimsAndRejectsTampering() {
        String valid = encode(claims -> { });
        assertThat(decoder.decode(valid).getSubject()).isEqualTo("123");
        String[] parts = valid.split("\\.");
        parts[2] = (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        assertThatThrownBy(() -> decoder.decode(String.join(".", parts))).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode("not-a-jwt")).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder.decode(RefreshToken.create().value())).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredFutureWrongIssuerAudienceAndRefreshClaims() {
        List<Consumer<JwtClaimsSet.Builder>> invalid = List.of(
                claims -> claims.expiresAt(now.minusSeconds(1)),
                claims -> claims.issuedAt(now.plusSeconds(1)),
                claims -> claims.issuer("other"),
                claims -> claims.audience(List.of("other")),
                claims -> claims.claims(map -> map.remove("aud")),
                claims -> claims.claim("token_use", "refresh"),
                claims -> claims.subject(""),
                claims -> claims.claim("sid", "forged"),
                claims -> claims.claim("roles", List.of("ADMIN")),
                claims -> claims.claim("roles", List.of()),
                claims -> claims.claims(map -> map.remove("exp")),
                claims -> claims.claims(map -> map.remove("iat")),
                claims -> claims.claim("email", ""));
        int mutationIndex = 0;
        for (var mutation : invalid) {
            String token = encode(mutation);
            assertThatThrownBy(() -> decoder.decode(token)).as("Invalid claim case %s", mutationIndex++)
                    .isInstanceOf(JwtException.class);
        }
    }

    @Test
    void rejectsOtherAlgorithmsEvenWithTheSameKey() {
        AuthProperties larger = properties(java.util.Base64.getEncoder().encodeToString(new byte[64]), true, "Lax");
        var key = config.jwtSigningKey(larger);
        var hs512 = config.jwtEncoder(key).encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS512).build(),
                claims().build())).getTokenValue();
        JwtDecoder hs256Only = config.jwtDecoder(key, larger, clock);
        assertThatThrownBy(() -> hs256Only.decode(hs512)).isInstanceOf(JwtException.class);
    }

    @Test
    void validatesKeyCookieAndDurationConfigurationAndUsesSecureCookies() {
        assertThatThrownBy(() -> config.jwtSigningKey(properties("c2hvcnQ=", true, "Lax")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties("", true, "Lax")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties(KEY, false, "None")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties(KEY, true, "invalid")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthProperties(KEY, "issuer", "audience", Duration.ZERO,
                Duration.ofDays(7), true, "Lax")).isInstanceOf(IllegalArgumentException.class);
        var response = new MockHttpServletResponse();
        var cookies = new AuthCookies(properties(KEY, true, "None"), clock);
        cookies.write(response, RefreshToken.create(), now.plusSeconds(60));
        assertThat(response.getHeader("Set-Cookie")).contains("HttpOnly", "Secure", "SameSite=None", "Max-Age=60")
                .contains("Path=/api/v1/auth");
        cookies.clear(response);
        assertThat(response.getHeaders("Set-Cookie").getLast()).contains("Max-Age=0");
    }

    @Test
    void refreshTokensHaveIndependentSecretsAndNeverRevealValuesViaToString() {
        RefreshToken first = RefreshToken.create();
        RefreshToken next = RefreshToken.rotate(first.sessionId());
        assertThat(first.value()).hasSize(76);
        assertThat(first.hash()).hasSize(64).isNotEqualTo(next.hash());
        assertThat(RefreshToken.parse(first.value()).hash()).isEqualTo(first.hash());
        assertThat(next.sessionId()).isEqualTo(first.sessionId());
        assertThat(first.toString()).doesNotContain(first.value());
        for (String invalid : new String[]{"", "jwt", first.sessionId() + ".forged"}) {
            assertThatThrownBy(() -> RefreshToken.parse(invalid)).isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
        }
    }

    private String encode(Consumer<JwtClaimsSet.Builder> change) {
        var claims = claims();
        change.accept(claims);
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }

    private JwtClaimsSet.Builder claims() {
        return JwtClaimsSet.builder().issuer("issuer").audience(List.of("audience")).subject("123")
                .issuedAt(now.minusSeconds(10)).expiresAt(now.plusSeconds(900)).claim("token_use", "access")
                .claim("email", "customer@example.com").claim("sid", "0123456789abcdef0123456789abcdef")
                .claim("roles", List.of("CUSTOMER"));
    }

    private AuthProperties properties(String key, boolean secure, String sameSite) {
        return new AuthProperties(key, "issuer", "audience", Duration.ofMinutes(15), Duration.ofDays(7), secure, sameSite);
    }
}
