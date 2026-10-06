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
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.session.web.http.CookieSerializer;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CookieSerializer cookieSerializer,
                                                   SecurityContextRepository contextRepository,
                                                   CsrfTokenRepository csrfTokenRepository)
            throws Exception {

        http
                .cors(withDefaults())
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .addFilterAfter(new SessionCookieRenewalFilter(cookieSerializer), AnonymousAuthenticationFilter.class)
                .requestCache(AbstractHttpConfigurer::disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
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
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()

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

                .formLogin(form -> form
                        .loginPage("/api/v1/auth/login")
                        .loginProcessingUrl("/api/v1/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) ->
                                response.setStatus(204))
                        .failureHandler((request, response, exception) ->
                                response.setStatus(401))
                )
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
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
