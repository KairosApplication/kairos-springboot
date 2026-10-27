package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.TestCompanyFactory;
import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.model.enums.Position;
import com.kairos.kairosapipostgres.repository.*;
import com.kairos.kairosapipostgres.security.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static com.kairos.kairosapipostgres.TestCsrf.cookieCsrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "test.auth.web=true")
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {
    private static final String PASSWORD = "test-password-123";
    private static final AtomicLong CPF = new AtomicLong(52998224725L);
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository users;
    @Autowired private CustomerRepository customers;
    @Autowired private EmployeeRepository employees;
    @Autowired private CompanyRepository companies;
    @Autowired private PasswordEncoder passwords;
    @MockitoBean private RedisRefreshTokenStore sessions;
    @Autowired private org.springframework.security.oauth2.jwt.JwtEncoder encoder;
    @Autowired private org.springframework.security.oauth2.jwt.JwtDecoder decoder;

    @BeforeEach
    void setup() {
        when(sessions.create(anyLong(), any(), any())).thenReturn(Instant.now().plusSeconds(604800));
    }

    @Test
    void loginReturnsBearerAndHttpOnlyRefreshWithoutHttpSession() throws Exception {
        User user = customer();
        MvcResult result = login(user.getEmail());
        var body = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.get("expiresIn").asLong()).isEqualTo(900);
        assertThat(body.has("refreshToken")).isFalse();
        MockCookie refresh = MockCookie.parse(result.getResponse().getHeader("Set-Cookie"));
        assertThat(refresh.getName()).isEqualTo(AuthCookies.REFRESH);
        assertThat(refresh.isHttpOnly()).isTrue();
        assertThat(refresh.getPath()).isEqualTo("/api/v1");
        assertThat(refresh.getSameSite()).isEqualTo("Lax");
        assertThat(refresh.getMaxAge()).isBetween(604790, 604800);
        assertThat(result.getRequest().getSession(false)).isNull();
        String access = access(result);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
        mvc.perform(get("/api/v1/products/list").header("Authorization", "Bearer " + access)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/employees/list").header("Authorization", "Bearer " + access)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie(AuthCookies.ACCESS, access))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + refresh.getValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidCredentialsAndUnassignedAccountReturn401() throws Exception {
        User customer = customer();
        for (String email : new String[]{customer.getEmail(), "missing@example.com", createUser().getEmail()}) {
            mvc.perform(post("/api/v1/auth/login").with(cookieCsrf(mvc, mapper)).param("email", email)
                            .param("password", email.equals(customer.getEmail()) ? "wrong" : PASSWORD))
                    .andExpect(status().isUnauthorized()).andExpect(cookie().doesNotExist(AuthCookies.REFRESH));
        }
        verify(sessions, never()).create(anyLong(), any(), any());
        mvc.perform(get("/api/v1/auth/me").with(httpBasic(customer.getEmail(), PASSWORD))).andExpect(status().isUnauthorized());
    }

    @Test
    void managerAndEmployeeRolesArePreserved() throws Exception {
        for (Position position : new Position[]{Position.MANAGER, Position.CASHIER}) {
            User user = createUser();
            employees.saveAndFlush(new Employee(null, position, user));
            String access = access(login(user.getEmail()));
            mvc.perform(get("/api/v1/products/list").header("Authorization", "Bearer " + access)).andExpect(status().isOk());
            mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                    .andExpect(jsonPath("$.roles[0]").value(position == Position.MANAGER ? "MANAGER" : "EMPLOYEE"));
        }
    }

    @Test
    void refreshReloadsCurrentRolesAndIssuesOnlyAfterAtomicRotation() throws Exception {
        User user = customer();
        var state = new RedisRefreshTokenStore.Session(user.getId(), Instant.now().plusSeconds(100));
        when(sessions.inspect(any())).thenReturn(state);
        when(sessions.rotate(any(), any())).thenReturn(state);
        employees.saveAndFlush(new Employee(null, Position.MANAGER, user));
        MvcResult result = mvc.perform(post("/api/v1/auth/refresh").with(cookieCsrf(mvc, mapper)).cookie(refreshCookie()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.refreshToken").doesNotExist()).andReturn();
        mvc.perform(get("/api/v1/employees/list").header("Authorization", "Bearer " + access(result))).andExpect(status().isOk());
        verify(sessions).rotate(any(), any());
        when(sessions.rotate(any(), any())).thenThrow(new org.springframework.security.authentication.BadCredentialsException("used"));
        mvc.perform(post("/api/v1/auth/refresh").with(cookieCsrf(mvc, mapper)).cookie(refreshCookie()))
                .andExpect(status().isUnauthorized()).andExpect(cookie().doesNotExist(AuthCookies.REFRESH));
    }

    @Test
    void removedUserProfileRevokesRefreshSession() throws Exception {
        User user = createUser();
        when(sessions.inspect(any())).thenReturn(new RedisRefreshTokenStore.Session(user.getId(), Instant.now().plusSeconds(100)));
        mvc.perform(post("/api/v1/auth/refresh").with(cookieCsrf(mvc, mapper)).cookie(refreshCookie())).andExpect(status().isUnauthorized());
        verify(sessions).revoke(any());
        verify(sessions, never()).rotate(any(), any());
    }

    @Test
    void redisOutageFailsClosedForLoginRefreshAndAccess() throws Exception {
        User user = customer();
        String access = access(login(user.getEmail()));
        when(sessions.create(anyLong(), any(), any())).thenThrow(new RedisConnectionFailureException("offline"));
        mvc.perform(post("/api/v1/auth/login").with(cookieCsrf(mvc, mapper)).param("email", user.getEmail()).param("password", PASSWORD))
                .andExpect(status().isServiceUnavailable()).andExpect(cookie().doesNotExist(AuthCookies.REFRESH))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
        when(sessions.inspect(any())).thenThrow(new RedisConnectionFailureException("offline"));
        mvc.perform(post("/api/v1/auth/refresh").with(cookieCsrf(mvc, mapper)).cookie(refreshCookie())).andExpect(status().isServiceUnavailable());
        doThrow(new RedisConnectionFailureException("offline")).when(sessions).assertActive(anyString(), anyString());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void csrfCookieRoundTripAndCookieEndpointsRequireCsrfEvenWithBearer() throws Exception {
        MvcResult csrfResult = mvc.perform(get("/api/v1/auth/login")).andExpect(status().isOk()).andReturn();
        Cookie cookie = MockCookie.parse(csrfResult.getResponse().getHeader("Set-Cookie"));
        var body = mapper.readTree(csrfResult.getResponse().getContentAsString());
        User user = customer();
        MvcResult logged = mvc.perform(post("/api/v1/auth/login").cookie(cookie)
                        .header(body.get("headerName").asText(), body.get("token").asText())
                        .param("email", user.getEmail()).param("password", PASSWORD)).andExpect(status().isOk()).andReturn();
        for (String path : new String[]{"login", "refresh", "logout"}) {
            mvc.perform(post("/api/v1/auth/" + path).header("Authorization", "Bearer " + access(logged)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/v1/auth/login").cookie(cookie).header("X-CSRF-TOKEN", "forged")
                        .param("email", user.getEmail()).param("password", PASSWORD)).andExpect(status().isForbidden());
    }

    @Test
    void logoutIsIdempotentClearsBothCookiesAndRejectsIssuedJwt() throws Exception {
        String access = access(login(customer().getEmail()));
        mvc.perform(post("/api/v1/auth/logout").with(cookieCsrf(mvc, mapper)).header("Authorization", "Bearer " + access))
                .andExpect(status().isNoContent()).andExpect(cookie().maxAge(AuthCookies.REFRESH, 0))
                .andExpect(cookie().maxAge(AuthCookies.ACCESS, 0));
        verify(sessions).revokeAuthenticatedSession(anyString());
        mvc.perform(post("/api/v1/auth/logout").with(cookieCsrf(mvc, mapper))).andExpect(status().isNoContent());
        doThrow(new org.springframework.security.authentication.BadCredentialsException("revoked"))
                .when(sessions).assertActive(anyString(), anyString());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access)).andExpect(status().isUnauthorized());
    }

    @Test
    void corsAllowsOnlyConfiguredOrigins() throws Exception {
        mvc.perform(options("/api/v1/auth/refresh").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,X-CSRF-TOKEN"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/v1/auth/refresh").header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }

    @Test
    void apiRenewsMissingOrExpiredAccessAndContinuesOriginalRequest() throws Exception {
        User user = customer();
        MvcResult login = login(user.getEmail());
        String access = access(login);
        String expired = expired(access);
        Cookie refresh = new Cookie(AuthCookies.REFRESH,
                MockCookie.parse(login.getResponse().getHeader("Set-Cookie")).getValue());
        var state = new RedisRefreshTokenStore.Session(user.getId(), Instant.now().plusSeconds(300));
        when(sessions.automatic(any(), any(), anyBoolean())).thenReturn(state);
        for (int mode = 0; mode < 3; mode++) {
            var request = get("/api/v1/auth/me").cookie(refresh);
            if (mode == 1) {
                request.cookie(new Cookie(AuthCookies.ACCESS, expired));
            }
            if (mode == 2) {
                request.header("Authorization", "Bearer " + expired);
            }
            mvc.perform(request).andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(user.getEmail()))
                    .andExpect(cookie().httpOnly(AuthCookies.ACCESS, true))
                    .andExpect(cookie().httpOnly(AuthCookies.REFRESH, true))
                    .andExpect(header().string("Cache-Control", "no-store"));
        }
        verify(sessions, times(3)).automatic(any(), any(), eq(true));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + expired)
                        .cookie(new Cookie(AuthCookies.ACCESS, access), refresh)).andExpect(status().isOk());
        verify(sessions, times(3)).automatic(any(), any(), eq(true));
    }

    @Test
    void invalidSignatureAndMismatchedSessionsNeverRenew() throws Exception {
        User user = customer();
        String access = access(login(user.getEmail()));
        for (String token : new String[]{"forged", expired(access)}) {
            mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token).cookie(refreshCookie()))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + expired(access)))
                .andExpect(status().isUnauthorized());
        verify(sessions, never()).automatic(any(), any(), anyBoolean());
    }

    @Test
    void cookieRequestsRequireCsrfEvenWhenBearerHeaderIsPresent() throws Exception {
        String access = access(login(customer().getEmail()));
        mvc.perform(post("/api/v1/products/create").header("Authorization", "Bearer " + access)
                        .cookie(new Cookie(AuthCookies.ACCESS, access)))
                .andExpect(status().isForbidden());
        verify(sessions, never()).assertActive(anyString(), anyString());
        verify(sessions, never()).automatic(any(), any(), anyBoolean());
    }

    @Test
    void jwtRequestsPreserveCsrfCookieUntilExplicitLogout() throws Exception {
        MvcResult logged = login(customer().getEmail());
        Cookie accessCookie = logged.getResponse().getCookie(AuthCookies.ACCESS);
        MvcResult protection = mvc.perform(get("/api/v1/auth/login")).andExpect(status().isOk()).andReturn();
        Cookie csrfCookie = protection.getResponse().getCookie(AuthCookies.CSRF);
        String token = mapper.readTree(protection.getResponse().getContentAsString()).get("token").asText();
        MvcResult authenticated = mvc.perform(get("/api/v1/auth/me").cookie(accessCookie, csrfCookie))
                .andExpect(status().isOk()).andReturn();
        assertThat(authenticated.getResponse().getHeaders("Set-Cookie"))
                .noneMatch(value -> value.startsWith(AuthCookies.CSRF + "="));
        mvc.perform(post("/api/v1/auth/logout").cookie(accessCookie, csrfCookie).header("X-CSRF-TOKEN", token))
                .andExpect(status().isNoContent()).andExpect(cookie().maxAge(AuthCookies.CSRF, 0));
    }

    @Test
    void expiredSignedAccessCanLogoutAndRedisFailurePreservesCookies() throws Exception {
        String expired = expired(access(login(customer().getEmail())));
        mvc.perform(post("/api/v1/auth/logout").with(cookieCsrf(mvc, mapper))
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isNoContent()).andExpect(cookie().maxAge(AuthCookies.ACCESS, 0));
        doThrow(new RedisConnectionFailureException("offline")).when(sessions).revokeAuthenticatedSession(anyString());
        mvc.perform(post("/api/v1/auth/logout").with(cookieCsrf(mvc, mapper))
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isServiceUnavailable()).andExpect(cookie().doesNotExist(AuthCookies.ACCESS));
    }

    @Test
    void chatContextRequiresAuthenticationAndCsrfWithCookiesOrBearer() throws Exception {
        User user = customer();
        String access = access(login(user.getEmail()));
        Cookie cookie = new Cookie(AuthCookies.ACCESS, access);
        mvc.perform(post("/api/v1/auth/chat-context").cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/chat-context").cookie(cookie)
                        .with(cookieCsrf(mvc, mapper)).header("X-CSRF-TOKEN", "invalid"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/chat-context").cookie(cookie).with(cookieCsrf(mvc, mapper)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
        mvc.perform(post("/api/v1/auth/chat-context").header("Authorization", "Bearer " + access))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/chat-context").header("Authorization", "Bearer " + access)
                        .with(cookieCsrf(mvc, mapper)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
    }

    @Test
    void chatContextRejectsAnonymousAndRevokedSessions() throws Exception {
        mvc.perform(post("/api/v1/auth/chat-context").with(cookieCsrf(mvc, mapper)))
                .andExpect(status().isUnauthorized());
        String access = access(login(customer().getEmail()));
        Cookie cookie = new Cookie(AuthCookies.ACCESS, access);
        mvc.perform(post("/api/v1/auth/logout").cookie(cookie).with(cookieCsrf(mvc, mapper)))
                .andExpect(status().isNoContent());
        verify(sessions).revokeAuthenticatedSession(anyString());
        doThrow(new org.springframework.security.authentication.BadCredentialsException("revoked"))
                .when(sessions).assertActive(anyString(), anyString());
        mvc.perform(post("/api/v1/auth/chat-context").cookie(cookie).with(cookieCsrf(mvc, mapper)))
                .andExpect(status().isUnauthorized());
    }

    private String expired(String token) {
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder()
                .claims(values -> values.putAll(decoder.decode(token).getClaims()))
                .issuedAt(Instant.now().minusSeconds(1000)).expiresAt(Instant.now().minusSeconds(10)).build();
        return encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(
                        org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private Cookie refreshCookie() {
        return new Cookie(AuthCookies.REFRESH, RefreshToken.create().value());
    }

    private User createUser() {
        long unique = CPF.getAndIncrement();
        return users.saveAndFlush(new User(null, "Ana", "Silva", LocalDate.of(2000, 1, 1),
                Long.toString(unique), unique + "@example.com", passwords.encode(PASSWORD)));
    }

    private User customer() {
        User user = createUser();
        customers.saveAndFlush(new Customer(null, user, TestCompanyFactory.create(companies)));
        return user;
    }

    private MvcResult login(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").with(cookieCsrf(mvc, mapper)).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", email).param("password", PASSWORD)).andExpect(status().isOk()).andReturn();
    }

    private String access(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
