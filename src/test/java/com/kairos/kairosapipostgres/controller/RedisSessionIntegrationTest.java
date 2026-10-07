package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.model.Customer;
import com.kairos.kairosapipostgres.TestCompanyFactory;
import com.kairos.kairosapipostgres.repository.CompanyRepository;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.repository.CustomerRepository;
import com.kairos.kairosapipostgres.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=",
        "spring.data.redis.url=redis://localhost:${test.redis.port:6379}",
        "spring.session.timeout=15d",
        "spring.session.data.redis.namespace=kairos:test:session",
        "spring.datasource.url=jdbc:h2:mem:kairos_redis;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "integration.redis", matches = "true")
class RedisSessionIntegrationTest {

    private static final long SESSION_SECONDS = Duration.ofDays(15).toSeconds();
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private CustomerRepository customers;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private StringRedisTemplate redis;
    @Autowired private CompanyRepository companies;

    @Test
    void shouldAuthenticatePublicRegistrationAndStoreCompanyAndSessionInRedis() throws Exception {
        var company = TestCompanyFactory.create(companies);
        String email = UUID.randomUUID() + "@example.com";
        CsrfSession csrf = csrf(null);
        MvcResult result = mvc.perform(post("/api/v1/users/registration").cookie(csrf.cookie())
                        .header(csrf.header(), csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ana", "lastName": "Silva", "birthDate": "2000-01-01",
                                  "password": "secret", "email": "%s", "cpf": "11144477735",
                                  "companyId": %d
                                }
                                """.formatted(email, company.getId())))
                .andExpect(status().isCreated()).andReturn();
        MockCookie cookie = responseCookie(result);
        assertThat(cookie.getValue()).isNotEqualTo(csrf.cookie().getValue());
        assertThat(redis.getExpire(sessionKey(cookie))).isBetween(SESSION_SECONDS - 10, SESSION_SECONDS);
        Long userId = users.findByEmail(email).orElseThrow().getId();
        assertThat(customers.findByUserId(userId).orElseThrow().getCompany().getId()).isEqualTo(company.getId());
        mvc.perform(get("/api/v1/auth/me").cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
        mvc.perform(post("/api/v1/auth/logout").cookie(cookie).header(csrf.header(), csrf.token()))
                .andExpect(status().isForbidden());
        CsrfSession renewed = csrf(cookie);
        mvc.perform(post("/api/v1/auth/logout").cookie(cookie).header(renewed.header(), renewed.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRestoreOnlyTheCookieSessionAndRenewRedisTtlAndCookie() throws Exception {
        LoginSession login = login();
        String key = sessionKey(login.cookie());
        assertThat(login.cookie().getMaxAge()).isEqualTo(SESSION_SECONDS);
        assertThat(login.cookie().isHttpOnly()).isTrue();
        assertThat(login.cookie().getSameSite()).isEqualTo("Lax");
        assertThat(redis.getExpire(key)).isBetween(SESSION_SECONDS - 10, SESSION_SECONDS);

        redis.expire(key, Duration.ofMinutes(1));
        MvcResult resumed = mvc.perform(get("/api/v1/auth/me").cookie(login.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(login.email()))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"))
                .andReturn();
        MockCookie renewed = responseCookie(resumed);
        assertThat(renewed.getValue()).isEqualTo(login.cookie().getValue());
        assertThat(renewed.getMaxAge()).isEqualTo(SESSION_SECONDS);
        assertThat(redis.getExpire(key)).isBetween(SESSION_SECONDS - 10, SESSION_SECONDS);

        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie("JSESSIONID",
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString()
                                .getBytes(StandardCharsets.UTF_8)))))
                .andExpect(status().isUnauthorized());
        redis.delete(key);
    }

    @Test
    void shouldRejectExpiredSessionEvenWhenAnotherLoginExistsInRedis() throws Exception {
        LoginSession expired = login();
        LoginSession active = login();
        redis.expire(sessionKey(expired.cookie()), Duration.ZERO);

        mvc.perform(get("/api/v1/auth/me").cookie(expired.cookie()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").cookie(active.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(active.email()));
        redis.delete(sessionKey(active.cookie()));
    }

    @Test
    void shouldRemoveSessionFromRedisOnLogout() throws Exception {
        LoginSession login = login();
        CsrfSession csrf = csrf(login.cookie());
        mvc.perform(post("/api/v1/auth/logout").cookie(login.cookie())
                        .header(csrf.header(), csrf.token()))
                .andExpect(status().isNoContent());
        assertThat(redis.hasKey(sessionKey(login.cookie()))).isFalse();
        mvc.perform(get("/api/v1/auth/me").cookie(login.cookie()))
                .andExpect(status().isUnauthorized());
    }

    private LoginSession login() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        User user = users.saveAndFlush(new User(null, "Ana", "Silva", LocalDate.of(2000, 1, 1),
                UUID.randomUUID().toString(), email, passwordEncoder.encode("secret")));
        customers.saveAndFlush(new Customer(null, user, TestCompanyFactory.create(companies)));
        CsrfSession csrf = csrf(null);
        MvcResult result = mvc.perform(post("/api/v1/auth/login").cookie(csrf.cookie())
                        .header(csrf.header(), csrf.token())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", email).param("password", "secret"))
                .andExpect(status().isNoContent()).andReturn();
        MockCookie authenticated = responseCookie(result);
        assertThat(authenticated.getValue()).isNotEqualTo(csrf.cookie().getValue());
        assertThat(redis.hasKey(sessionKey(csrf.cookie()))).isFalse();
        return new LoginSession(email, authenticated);
    }

    private CsrfSession csrf(Cookie cookie) throws Exception {
        var request = get("/api/v1/auth/login");
        if (cookie != null) {
            request.cookie(cookie);
        }
        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CsrfSession(responseCookie(result), body.get("headerName").asText(), body.get("token").asText());
    }

    private MockCookie responseCookie(MvcResult result) {
        return result.getResponse().getHeaders("Set-Cookie").stream()
                .map(MockCookie::parse)
                .filter(cookie -> "JSESSIONID".equals(cookie.getName()) && cookie.getMaxAge() > 0)
                .findFirst().orElseThrow();
    }

    private String sessionKey(Cookie cookie) {
        String id = new String(Base64.getDecoder().decode(cookie.getValue()), StandardCharsets.UTF_8);
        return "kairos:test:session:sessions:" + id;
    }

    private record LoginSession(String email, MockCookie cookie) {
    }

    private record CsrfSession(MockCookie cookie, String header, String token) {
    }
}
