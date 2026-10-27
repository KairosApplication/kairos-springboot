package com.kairos.kairosapipostgres.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class JwtConfig {
    @Bean
    public Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecretKey jwtSigningKey(AuthProperties properties) {
        byte[] bytes = Base64.getDecoder().decode(properties.signingKey());
        if (bytes.length < 32) {
            throw new IllegalArgumentException("AUTH_SIGNING_KEY must contain at least 256 bits, Base64 encoded");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    @Primary
    public JwtDecoder jwtDecoder(SecretKey key, AuthProperties properties, Clock clock) {
        return decoder(key, properties, clock, false);
    }

    @Bean
    public JwtDecoder sessionProofDecoder(SecretKey key, AuthProperties properties, Clock clock) {
        return decoder(key, properties, clock, true);
    }

    private JwtDecoder decoder(SecretKey key, AuthProperties properties, Clock clock, boolean allowExpired) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamp = new JwtTimestampValidator(Duration.ZERO);
        timestamp.setClock(clock);
        OAuth2TokenValidator<Jwt> claims = jwt -> {
            String subject = jwt.getSubject();
            String sid = jwt.getClaimAsString("sid");
            String email = jwt.getClaimAsString("email");
            List<String> roles = jwt.getClaimAsStringList("roles");
            var expires = jwt.getExpiresAt();
            var issued = jwt.getIssuedAt();
            var audiences = jwt.getAudience();
            var now = clock.instant();
            boolean valid = expires != null && issued != null
                    && (allowExpired || expires.isAfter(now))
                    && expires.isAfter(issued)
                    && !issued.isAfter(now)
                    && subject != null && subject.matches("[1-9][0-9]*")
                    && sid != null && sid.matches("[a-f0-9]{32}")
                    && email != null && !email.isBlank()
                    && "access".equals(jwt.getClaimAsString("token_use"))
                    && audiences != null && audiences.contains(properties.audience())
                    && roles != null && !roles.isEmpty()
                    && roles.stream().allMatch(role -> role != null
                            && List.of("CUSTOMER", "EMPLOYEE", "MANAGER").contains(role));
            return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid access token claims", null));
        };
        decoder.setJwtValidator(allowExpired
                ? new DelegatingOAuth2TokenValidator<>(new JwtIssuerValidator(properties.issuer()), claims)
                : new DelegatingOAuth2TokenValidator<>(timestamp, new JwtIssuerValidator(properties.issuer()), claims));
        return decoder;
    }
}
