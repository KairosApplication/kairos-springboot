package com.kairos.kairosapipostgres;

import com.kairos.kairosapipostgres.service.DailyActivityService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "integration.database", matches = "postgresql")
class DailyActivityPostgresIntegrationTest {
    private static final org.testcontainers.containers.GenericContainer<?> REDIS =
            new org.testcontainers.containers.GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:tc:postgresql:17-alpine:///kairos_dau_test");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        REDIS.start();
        registry.add("spring.data.redis.url", () -> "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private DailyActivityService activity;
    @Autowired private MeterRegistry meters;
    @Autowired private tools.jackson.databind.ObjectMapper json;
    @Autowired private com.kairos.kairosapipostgres.service.InventoryService inventories;
    private int company;
    private int otherCompany;
    private int user;

    @BeforeEach
    void fixture() {
        company = company();
        otherCompany = company();
        user = user();
        jdbc.update("INSERT INTO employees(users_id,company_id,position) VALUES(?,?,'MANAGER')", user, company);
    }

    private int company() {
        String unique = UUID.randomUUID().toString();
        return jdbc.queryForObject("INSERT INTO companies(name,cnpj,email,plan) VALUES('DAU DEMO',?,?,'BASIC') RETURNING id",
                Integer.class, unique.substring(0, 18), unique + "@demo.invalid");
    }

    private int user() {
        String unique = UUID.randomUUID().toString();
        return jdbc.queryForObject("""
                INSERT INTO users(name,last_name,birth_date,cpf,email,password)
                VALUES('DAU','FICTICIO','2000-01-01',?,?,'{noop}demo-only') RETURNING id
                """, Integer.class, unique.substring(0, 14), unique + "@demo.invalid");
    }

    private int count() {
        return jdbc.queryForObject("SELECT count(*) FROM user_accesses WHERE company_id=?", Integer.class, company);
    }

    private void requestAs(int id) throws Exception {
        mvc.perform(get("/api/v1/auth/me").param("user_id", "999999").param("company_id", "999999")
                        .with(jwt().jwt(token -> token.subject(Integer.toString(id)).claim("email", "demo@demo.invalid"))))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedSubjectAndDatabaseMembershipDeduplicateAndPreserveFirstSeen() throws Exception {
        requestAs(user);
        var first = jdbc.queryForObject("SELECT first_seen_at FROM user_accesses WHERE user_id=?", OffsetDateTime.class, user);
        var last = jdbc.queryForObject("SELECT last_seen_at FROM user_accesses WHERE user_id=?", OffsetDateTime.class, user);
        requestAs(user);
        assertThat(count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT first_seen_at FROM user_accesses WHERE user_id=?", OffsetDateTime.class, user)).isEqualTo(first);
        assertThat(jdbc.queryForObject("SELECT last_seen_at FROM user_accesses WHERE user_id=?", OffsetDateTime.class, user)).isAfter(last);
        assertThat(jdbc.queryForObject("SELECT company_id FROM user_accesses WHERE user_id=?", Integer.class, user)).isEqualTo(company);
    }

    @Test
    void twoDistinctUsersProduceDauTwo() throws Exception {
        int second = user();
        jdbc.update("INSERT INTO customers(users_id,company_id) VALUES(?,?)", second, company);
        requestAs(user);
        requestAs(second);
        assertThat(jdbc.queryForObject("SELECT daily_active_users FROM vw_dau_daily WHERE company_id=?", Long.class, company)).isEqualTo(2);
    }

    @Test
    void membershipIsCheckedAndTwoAuthorizedCompaniesRemainSeparate() {
        assertThatThrownBy(() -> jdbc.queryForObject("SELECT register_user_access(?,?)", Object.class, user, otherCompany))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        jdbc.update("INSERT INTO customers(users_id,company_id) VALUES(?,?)", user, otherCompany);
        activity.record(user);
        assertThat(count()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_accesses WHERE user_id=? AND company_id=?", Integer.class, user, otherCompany)).isEqualTo(1);
        int unlinked = user();
        activity.record(unlinked);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_accesses WHERE user_id=?", Integer.class, unlinked)).isZero();
    }

    @Test
    void anonymousInvalidLoginHealthAndBotsDoNotCount() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").with(csrf()).param("email", "missing@demo.invalid").param("password", "wrong"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("User-Agent", "demo-crawler")
                .with(jwt().jwt(token -> token.subject(Integer.toString(user))))).andExpect(status().isOk());
        assertThat(count()).isZero();
    }

    @Test
    void concurrentEventsHaveOneDailyRow() throws Exception {
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tasks = java.util.stream.IntStream.range(0, 20)
                    .<java.util.concurrent.Callable<Void>>mapToObj(i -> () -> {
                        activity.record(user);
                        return null;
                    }).toList();
            for (var result : executor.invokeAll(tasks)) {
                result.get();
            }
        }
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void previousDayIsPreservedAndTodayGetsNewRecord() {
        jdbc.update("""
                INSERT INTO user_accesses(user_id,company_id,access_date)
                VALUES(?,?,(clock_timestamp() AT TIME ZONE 'America/Sao_Paulo')::date-1)
                """, user, company);
        activity.record(user);
        assertThat(count()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT access_date) FROM user_accesses WHERE user_id=?", Integer.class, user)).isEqualTo(2);
    }

    @Test
    void metricsDatabaseFailureDoesNotBreakAuthenticatedResponse() throws Exception {
        double before = meters.counter("kairos.dau.record.failures").count();
        jdbc.execute("ALTER FUNCTION register_user_access(integer,integer) RENAME TO test_disabled_user_access");
        try {
            requestAs(user);
            assertThat(meters.counter("kairos.dau.record.failures").count()).isGreaterThan(before);
            assertThat(count()).isZero();
        } finally {
            jdbc.execute("ALTER FUNCTION test_disabled_user_access(integer,integer) RENAME TO register_user_access");
        }
        requestAs(user);
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void inventoryCreationUsesSectorCompanyAndCrossCompanyUpdateIsRefused() {
        int sector = jdbc.queryForObject("INSERT INTO sectors(company_id,name,type) VALUES(?,'DAU stock','STORAGE') RETURNING id", Integer.class, company);
        int other = jdbc.queryForObject("INSERT INTO sectors(company_id,name,type) VALUES(?,'Other stock','STORAGE') RETURNING id", Integer.class, otherCompany);
        var saved = inventories.save(new com.kairos.kairosapipostgres.dto.request.InventoryRequest((long) sector));
        assertThat(jdbc.queryForObject("SELECT company_id FROM inventories WHERE id=?", Integer.class, saved.id())).isEqualTo(company);
        assertThatThrownBy(() -> inventories.update(saved.id(), new com.kairos.kairosapipostgres.dto.request.InventoryUpdateRequest((long) other)))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(jdbc.queryForObject("SELECT sector_id FROM inventories WHERE id=?", Integer.class, saved.id())).isEqualTo(sector);
    }

    @Test
    void realLoginSignedBearerAndRedisSessionLeadToPersistedDailyActivity() throws Exception {
        String email = jdbc.queryForObject("SELECT email FROM users WHERE id=?", String.class, user);
        var login = mvc.perform(post("/api/v1/auth/login").with(csrf())
                .param("email", email).param("password", "demo-only"))
                .andExpect(status().isOk()).andReturn();
        assertThat(count()).isZero(); // Login itself is not a human activity event.
        String token = json.readTree(login.getResponse().getContentAsString()).get("accessToken").asString();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(count()).isEqualTo(1);
    }
}
