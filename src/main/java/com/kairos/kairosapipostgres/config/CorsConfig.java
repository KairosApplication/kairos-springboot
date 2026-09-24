package com.kairos.kairosapipostgres.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:5173"
        ));

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
        source.registerCorsConfiguration("/api/v1/aisles/**", config);
        source.registerCorsConfiguration("/api/v1/shelves/**", config);
        source.registerCorsConfiguration("/api/v1/employee-aisles/**", config);
        source.registerCorsConfiguration("/api/v1/shelf-employees/**", config);
        source.registerCorsConfiguration("/api/v1/product-shelves/**", config);
        source.registerCorsConfiguration("/api/v1/inventories/**", config);
        source.registerCorsConfiguration("/api/v1/restockings/**", config);
        source.registerCorsConfiguration("/api/v1/alerts/**", config);
        source.registerCorsConfiguration("/api/v1/promotions/**", config);
        source.registerCorsConfiguration("/api/v1/auth/**", config);

        return source;
    }
}
