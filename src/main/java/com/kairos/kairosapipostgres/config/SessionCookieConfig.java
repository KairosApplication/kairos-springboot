package com.kairos.kairosapipostgres.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionCookieConfig {

    @Bean
    public CookieSerializer sessionCookieSerializer(@Value("${spring.session.timeout:15d}") String timeout,
                                                    @Value("${server.servlet.session.cookie.secure:}") String secure) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("JSESSIONID");
        serializer.setCookiePath("/");
        serializer.setCookieMaxAge(Math.toIntExact(DurationStyle.detectAndParse(timeout).toSeconds()));
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        if (!secure.isBlank()) {
            serializer.setUseSecureCookie(Boolean.parseBoolean(secure));
        }
        return serializer;
    }
}
