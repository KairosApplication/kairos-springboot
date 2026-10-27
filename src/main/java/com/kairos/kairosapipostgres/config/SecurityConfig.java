package com.kairos.kairosapipostgres.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.kairos.kairosapipostgres.security.AuthCookies;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.kairos.kairosapipostgres.security.ApiAuthenticationFilter;
import com.kairos.kairosapipostgres.security.RedisRefreshTokenStore;
import com.kairos.kairosapipostgres.service.TokenAuthService;
import org.springframework.beans.factory.annotation.Qualifier;
import java.time.Clock;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository csrfTokenRepository,
                                                   JwtDecoder jwtDecoder,
                                                   @Qualifier("sessionProofDecoder") JwtDecoder proofDecoder,
                                                   RedisRefreshTokenStore sessions, TokenAuthService tokens,
                                                   AuthCookies cookies, Clock clock)
            throws Exception {

        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("email");
        converter.setJwtGrantedAuthoritiesConverter(roles);
        var authentication = new ApiAuthenticationFilter(jwtDecoder, proofDecoder, converter, sessions, tokens, cookies, clock);

        http
                .cors(withDefaults())
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository)
                        // Login/logout explicitly rotate CSRF. A JWT request must preserve it.
                        .sessionAuthenticationStrategy(
                                new org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy())
                        .requireCsrfProtectionMatcher(request -> {
                            if (!CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request)) {
                                return false;
                            }
                            String path = request.getServletPath();
                            if (path.isEmpty()) {
                                path = request.getRequestURI().substring(request.getContextPath().length());
                            }
                            // Cookie authentication endpoints always require CSRF, even with a Bearer header.
                            if (path.startsWith("/api/v1/auth/")
                                    || path.equals("/api/v1/users/registration")
                                    || cookies.accessValue(request) != null || cookies.refreshValue(request) != null) {
                                return true;
                            }
                            String authorization = request.getHeader("Authorization");
                            return authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7);
                        }))
                .requestCache(AbstractHttpConfigurer::disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/health",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/registration").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/chat-context").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**")
                        .hasAnyRole("CUSTOMER", "EMPLOYEE", "MANAGER")
                        .requestMatchers("/api/v1/products/**").hasRole("MANAGER")
                        .requestMatchers("/api/v1/employees/**")
                        .hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/purchases/**")
                        .hasRole("MANAGER")

                        .requestMatchers(HttpMethod.GET, "/api/v1/aisles/**")
                        .hasAnyRole("MANAGER", "EMPLOYEE")
                        .requestMatchers("/api/v1/aisles/**")
                        .hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/shelves/**")
                        .hasAnyRole("MANAGER", "EMPLOYEE")
                        .requestMatchers("/api/v1/shelves/**")
                        .hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/employee-aisles/**")
                        .hasAnyRole("MANAGER", "EMPLOYEE")
                        .requestMatchers("/api/v1/employee-aisles/**")
                        .hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/shelf-employees/**")
                        .hasAnyRole("MANAGER", "EMPLOYEE")
                        .requestMatchers("/api/v1/shelf-employees/**")
                        .hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/product-shelves/**")
                        .hasAnyRole("MANAGER", "EMPLOYEE")
                        .requestMatchers("/api/v1/product-shelves/**")
                        .hasRole("MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventories/**")
                        .hasAnyRole("MANAGER", "EMPLOYEE")
                        .requestMatchers("/api/v1/inventories/**")
                        .hasRole("MANAGER")
                        .requestMatchers("/api/v1/restockings/**")
                        .hasAnyRole("MANAGER", "EMPLOYEE")
                        .requestMatchers("/api/v1/alerts/**")
                        .hasAnyRole("MANAGER", "EMPLOYEE")

                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**",
                                "/api/v1/sectors/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/*/recommendations")
                        .hasAnyRole("CUSTOMER", "MANAGER")
                        .requestMatchers("/api/v1/categories/**", "/api/v1/sectors/**",
                                "/api/v1/customers/**", "/api/v1/users/**")
                        .hasRole("MANAGER")

                        .anyRequest().denyAll()
                )

                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(authentication, AnonymousAuthenticationFilter.class)
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                response.setStatus(401))
                        .accessDeniedHandler((request, response, exception) ->
                                response.setStatus(403))
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository(AuthProperties properties) {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();
        repository.setCookieName(AuthCookies.CSRF);
        repository.setHeaderName("X-CSRF-TOKEN");
        repository.setCookiePath("/api/v1");
        repository.setCookieCustomizer(cookie -> cookie.httpOnly(true).secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite()));
        return repository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
