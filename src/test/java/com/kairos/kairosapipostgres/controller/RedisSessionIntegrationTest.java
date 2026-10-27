package com.kairos.kairosapipostgres.controller;

import com.kairos.kairosapipostgres.TestCompanyFactory;
import com.kairos.kairosapipostgres.model.Customer;
import com.kairos.kairosapipostgres.model.User;
import com.kairos.kairosapipostgres.repository.*;
import com.kairos.kairosapipostgres.security.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;
import static com.kairos.kairosapipostgres.TestCsrf.cookieCsrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Requires a real Redis. CI uses a service; local runs can opt into Testcontainers. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfSystemProperty(named = "integration.redis", matches = "true")
class RedisSessionIntegrationTest {
    private static final AtomicLong CPF = new AtomicLong(62998224725L);
    private static GenericContainer<?> container;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UserRepository users;
    @Autowired private CustomerRepository customers;
    @Autowired private CompanyRepository companies;
    @Autowired private PasswordEncoder passwords;
    @Autowired private StringRedisTemplate redis;
    @Autowired private RedisRefreshTokenStore sessions;
    @Autowired private javax.crypto.SecretKey signingKey;
    private final Set<String> createdKeys = new HashSet<>();

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        if (Boolean.getBoolean("integration.redis.container")) {
            container = new GenericContainer<>(DockerImageName.parse("redis:8.6-alpine")).withExposedPorts(6379);
            container.start();
            registry.add("spring.data.redis.url", () -> "redis://" + container.getHost() + ":" + container.getMappedPort(6379));
        } else {
            registry.add("spring.data.redis.url", () -> "redis://127.0.0.1:" + System.getProperty("test.redis.port", "6379"));
        }
    }

    @AfterEach
    void removeOnlySessionsCreatedByThisTest() {
        if (!createdKeys.isEmpty()) {
            redis.delete(createdKeys);
        }
    }

    @AfterAll
    static void stopIsolatedContainer() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    void fullLoginRotationReuseAndLogoutInvalidateAccessImmediately() throws Exception {
        User user = customer();
        Login first = login(user);
        Login otherDevice = login(user);
        String key = track(first.refresh());
        assertThat(redis.getExpire(key)).isBetween(604790L, 604800L);
        assertThat(redis.opsForHash().entries(key).toString()).doesNotContain(first.refresh().value());
        MvcResult refreshed = mvc.perform(post("/api/v1/auth/refresh").with(cookieCsrf(mvc, mapper)).cookie(tokenCookie(first.refresh())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.refreshToken").doesNotExist()).andReturn();
        RefreshToken second = refresh(refreshed);
        assertThat(second.value()).isNotEqualTo(first.refresh().value());
        assertThat(redis.opsForHash().get(key, "t:" + first.refresh().hash())).isEqualTo("used");
        assertThat(redis.opsForHash().get(key, "t:" + second.hash())).isEqualTo("active");
        mvc.perform(post("/api/v1/auth/refresh").with(cookieCsrf(mvc, mapper)).cookie(tokenCookie(first.refresh()))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").with(cookieCsrf(mvc, mapper)).cookie(tokenCookie(second))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").with(cookieCsrf(mvc, mapper)).cookie(tokenCookie(otherDevice.refresh()))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + first.access())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout").with(cookieCsrf(mvc, mapper)).header("Authorization", "Bearer " + otherDevice.access()))
                .andExpect(status().isNoContent()).andExpect(cookie().maxAge(AuthCookies.REFRESH, 0));
        assertThat(redis.opsForHash().get(RedisRefreshTokenStore.key(otherDevice.refresh().sessionId()), "revoked")).isEqualTo("1");
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + otherDevice.access()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownHashCannotRevokeEvenWhenLocatorIsKnown() {
        RefreshToken valid = create(Duration.ofMinutes(1));
        RefreshToken forged = RefreshToken.rotate(valid.sessionId());
        assertThatThrownBy(() -> sessions.inspect(forged)).isInstanceOf(BadCredentialsException.class);
        sessions.revoke(forged);
        assertThat(sessions.inspect(valid).userId()).isEqualTo(123);
        RefreshToken unknown = RefreshToken.create();
        assertThatThrownBy(() -> sessions.inspect(unknown)).isInstanceOf(BadCredentialsException.class);
        assertThat(sessions.inspect(valid).userId()).isEqualTo(123);
    }

    @Test
    void rotationPreservesAbsoluteExpiryAndAllConsumedHashesUntilExpiry() throws Exception {
        RefreshToken first = create(Duration.ofSeconds(4));
        String key = track(first);
        long initialTtl = redis.getExpire(key, TimeUnit.MILLISECONDS);
        Instant expires = sessions.inspect(first).expiresAt();
        Thread.sleep(150);
        RefreshToken second = RefreshToken.rotate(first.sessionId());
        assertThat(sessions.rotate(first, second).expiresAt()).isEqualTo(expires);
        assertThat(redis.getExpire(key, TimeUnit.MILLISECONDS)).isLessThan(initialTtl);
        assertThat(redis.opsForHash().hasKey(key, "t:" + first.hash())).isTrue();
        Thread.sleep(4100);
        assertThatThrownBy(() -> sessions.inspect(second)).isInstanceOf(BadCredentialsException.class);
        assertThat(redis.hasKey(key)).isFalse();
    }

    @Test
    void concurrentInstancesAcceptOnlyOneRotationThenRevokeTheFamilyForReuse() throws Exception {
        RefreshToken first = create(Duration.ofMinutes(1));
        RefreshToken nextA = RefreshToken.rotate(first.sessionId());
        RefreshToken nextB = RefreshToken.rotate(first.sessionId());
        RedisRefreshTokenStore secondInstance = new RedisRefreshTokenStore(new StringRedisTemplate(redis.getConnectionFactory()));
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var a = executor.submit(() -> attemptRotation(sessions, first, nextA, barrier));
            var b = executor.submit(() -> attemptRotation(secondInstance, first, nextB, barrier));
            assertThat((a.get(10, TimeUnit.SECONDS) ? 1 : 0) + (b.get(10, TimeUnit.SECONDS) ? 1 : 0)).isEqualTo(1);
        }
        assertThatThrownBy(() -> sessions.inspect(nextA)).isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> sessions.inspect(nextB)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logoutRacingRotationAlwaysPreventsFurtherRefresh() throws Exception {
        RefreshToken first = create(Duration.ofMinutes(1));
        RefreshToken next = RefreshToken.rotate(first.sessionId());
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var rotate = executor.submit(() -> attemptRotation(sessions, first, next, barrier));
            var logout = executor.submit(() -> {
                barrier.await();
                sessions.revoke(first);
                return true;
            });
            rotate.get(10, TimeUnit.SECONDS);
            logout.get(10, TimeUnit.SECONDS);
        }
        assertThatThrownBy(() -> sessions.inspect(next)).isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> sessions.inspect(first)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logoutWithDifferentCookieRevokesOnlyTheVerifiedJwtSession() throws Exception {
        User user = customer();
        Login current = login(user);
        Login other = login(user);
        mvc.perform(post("/api/v1/auth/logout").with(cookieCsrf(mvc, mapper))
                        .header("Authorization", "Bearer " + current.access()).cookie(tokenCookie(other.refresh())))
                .andExpect(status().isNoContent());
        assertThatThrownBy(() -> sessions.inspect(current.refresh())).isInstanceOf(BadCredentialsException.class);
        assertThat(sessions.inspect(other.refresh()).userId()).isEqualTo(user.getId());
    }

    @Test
    void logoutUsingOnlyRefreshCookieRevokesAndIsIdempotent() throws Exception {
        Login logged = login(customer());
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/auth/logout").with(cookieCsrf(mvc, mapper)).cookie(tokenCookie(logged.refresh())))
                    .andExpect(status().isNoContent());
        }
        mvc.perform(post("/api/v1/auth/refresh").with(cookieCsrf(mvc, mapper)).cookie(tokenCookie(logged.refresh()))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + logged.access())).andExpect(status().isUnauthorized());
    }

    @Test
    void browserAutomaticallyRotatesAndRejectsAccessAfterCookieLogout() throws Exception {
        Login logged = login(customer());
        MvcResult renewed = mvc.perform(get("/api/v1/auth/me").cookie(tokenCookie(logged.refresh())))
                .andExpect(status().isOk()).andExpect(cookie().httpOnly(AuthCookies.ACCESS, true)).andReturn();
        RefreshToken next = refresh(renewed);
        MvcResult parallel = mvc.perform(get("/api/v1/auth/me").cookie(tokenCookie(logged.refresh())))
                .andExpect(status().isOk()).andReturn();
        assertThat(refresh(parallel).value()).isEqualTo(next.value());
        String access = renewed.getResponse().getCookie(AuthCookies.ACCESS).getValue();
        mvc.perform(post("/api/v1/auth/logout").with(cookieCsrf(mvc, mapper)).cookie(tokenCookie(next)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").cookie(new Cookie(AuthCookies.ACCESS, access)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/me").cookie(tokenCookie(next))).andExpect(status().isUnauthorized());
    }

    @Test
    void automaticRotationIsAtomicAcrossInstancesAndReuseAfterOverlapRevokesAccess() throws Exception {
        RefreshToken first = create(Duration.ofMinutes(1));
        RefreshToken next = first.automaticSuccessor(signingKey);
        RedisRefreshTokenStore other = new RedisRefreshTokenStore(new StringRedisTemplate(redis.getConnectionFactory()));
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var a = executor.submit(() -> {
                barrier.await();
                return sessions.automatic(first, next, true);
            });
            var b = executor.submit(() -> {
                barrier.await();
                return other.automatic(first, first.automaticSuccessor(signingKey), true);
            });
            assertThat(a.get(10, TimeUnit.SECONDS).expiresAt()).isEqualTo(b.get(10, TimeUnit.SECONDS).expiresAt());
        }
        assertThat(sessions.inspect(next).userId()).isEqualTo(123);
        sessions.assertActive(first.sessionId(), "123");
        assertThatThrownBy(() -> sessions.assertActive(first.sessionId(), "999")).isInstanceOf(BadCredentialsException.class);
        // Force the overlap deadline into the past without waiting on wall-clock timing.
        redis.opsForHash().put(RedisRefreshTokenStore.key(first.sessionId()), "overlap:" + first.hash(), "0");
        assertThatThrownBy(() -> sessions.automatic(first, next, false)).isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> sessions.assertActive(first.sessionId(), "123")).isInstanceOf(BadCredentialsException.class);
    }

    private boolean attemptRotation(RedisRefreshTokenStore store, RefreshToken previous, RefreshToken next,
                                     CyclicBarrier barrier) throws Exception {
        barrier.await(5, TimeUnit.SECONDS);
        try {
            store.rotate(previous, next);
            return true;
        } catch (BadCredentialsException exception) {
            return false;
        }
    }

    private RefreshToken create(Duration ttl) {
        RefreshToken token = RefreshToken.create();
        track(token);
        sessions.create(123, token, ttl);
        return token;
    }

    private String track(RefreshToken token) {
        String key = RedisRefreshTokenStore.key(token.sessionId());
        createdKeys.add(key);
        return key;
    }

    private Cookie tokenCookie(RefreshToken token) {
        return new Cookie(AuthCookies.REFRESH, token.value());
    }

    private RefreshToken refresh(MvcResult result) {
        RefreshToken token = RefreshToken.parse(MockCookie.parse(result.getResponse().getHeader("Set-Cookie")).getValue());
        track(token);
        return token;
    }

    private Login login(User user) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login").with(cookieCsrf(mvc, mapper))
                        .param("email", user.getEmail()).param("password", "test-password"))
                .andExpect(status().isOk()).andReturn();
        return new Login(mapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText(), refresh(result));
    }

    private User customer() {
        long unique = CPF.getAndIncrement();
        User user = users.saveAndFlush(new User(null, "Ana", "Silva", LocalDate.of(2000, 1, 1),
                Long.toString(unique), unique + "@example.com", passwords.encode("test-password")));
        customers.saveAndFlush(new Customer(null, user, TestCompanyFactory.create(companies)));
        return user;
    }

    private record Login(String access, RefreshToken refresh) {
    }
}
