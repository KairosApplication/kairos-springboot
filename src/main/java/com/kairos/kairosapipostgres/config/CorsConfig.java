package com.kairos.kairosapipostgres.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String origins) {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(Arrays.stream(origins.split(","))
                .map(String::trim)
                .toList());

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-CSRF-TOKEN"
        ));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/v1/users/**", config);
        source.registerCorsConfiguration("/api/v1/employees/**", config);
        source.registerCorsConfiguration("/api/v1/customers/**", config);
        source.registerCorsConfiguration("/api/v1/categories/**", config);
        source.registerCorsConfiguration("/api/v1/products/**", config);
        source.registerCorsConfiguration("/api/v1/sectors/**", config);
        source.registerCorsConfiguration("/api/v1/auth/**", config);

        return source;
    }
}
