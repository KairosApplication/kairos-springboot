package com.kairos.kairosapipostgres.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .cors(withDefaults())
                .csrf(withDefaults())
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
                        .hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/employees/**")
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

                        .requestMatchers("/api/v1/categories/**").authenticated()
                        .requestMatchers("/api/v1/sectors/**").authenticated()
                        .requestMatchers("/api/v1/customers/**").authenticated()
                        .requestMatchers("/api/v1/users/**").authenticated()

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
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
