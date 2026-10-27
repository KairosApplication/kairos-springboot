package com.kairos.kairosapipostgres.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(RestockingRedisProperties.class)
public class RestockingRedisConfig {
    @Bean("restockingRedisHttp")
    public RestClient restockingRedisHttp(RestockingRedisProperties properties) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return RestClient.builder().baseUrl(properties.url().toString()).requestFactory(factory)
                .defaultHeader("X-Internal-API-Key", properties.internalKey() == null ? "" : properties.internalKey()).build();
    }
}
