package com.kairos.kairosapipostgres;

import jakarta.servlet.http.Cookie;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercises the actual cookie repository without replacing it with Spring's test session repository. */
public final class TestCsrf {
    private TestCsrf() {
    }

    public static RequestPostProcessor cookieCsrf(MockMvc mvc, ObjectMapper mapper) throws Exception {
        var result = mvc.perform(get("/api/v1/auth/login")).andExpect(status().isOk()).andReturn();
        var cookie = MockCookie.parse(result.getResponse().getHeader("Set-Cookie"));
        var body = mapper.readTree(result.getResponse().getContentAsString());
        return request -> {
            var cookies = new ArrayList<Cookie>();
            if (request.getCookies() != null) {
                cookies.addAll(Arrays.asList(request.getCookies()));
            }
            cookies.add(cookie);
            request.setCookies(cookies.toArray(Cookie[]::new));
            request.addHeader(body.get("headerName").asText(), body.get("token").asText());
            return request;
        };
    }
}
