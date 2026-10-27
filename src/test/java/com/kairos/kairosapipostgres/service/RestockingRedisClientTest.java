package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.config.*;
import com.kairos.kairosapipostgres.dto.response.RestockingCacheEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import java.net.URI;
import java.time.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RestockingRedisClientTest {
    private static final String KEY = "test-fixture-internal-key-not-a-real-secret";
    private final RestockingRedisProperties properties = new RestockingRedisProperties(true,
            URI.create("http://redis-history:8001"), KEY, Duration.ofSeconds(1), Duration.ofSeconds(2));

    @Test
    void sendsTheExactUpstreamContractAndAcceptsDuplicateAcknowledgements() {
        var builder = new RestockingRedisConfig().restockingRedisHttp(properties).mutate();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new RestockingRedisClient(builder.build());
        server.expect(requestTo("http://redis-history:8001/v1/restockings/events"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("X-Internal-API-Key", KEY))
                .andExpect(headerDoesNotExist("Authorization"))
                .andExpect(content().json("""
                        {"restocking_id":991,"company_id":5,"employee_id":72,"product_id":10,
                         "shelf_id":4,"inventory_id":3,"restocked_quantity":30,"qnt_avarias":2,
                         "restocking_date":"2026-10-19T10:30:00-03:00","product_name":"Arroz"}
                        """))
                .andRespond(withSuccess("""
                        {"cached":false,"duplicate":true,"week_start":"2026-10-19T00:00:00-03:00",
                         "expires_at":"2026-10-26T00:00:00-03:00"}
                        """, MediaType.APPLICATION_JSON));
        client.publish(new RestockingCacheEvent(991, 5, 72, 10, 4, 3, 30, 2,
                OffsetDateTime.parse("2026-10-19T10:30:00-03:00"), "Arroz"));
        server.verify();
    }

    @Test
    void readsTheTenantScopedWeekAndPropagatesFailuresToTheFallbackLayer() {
        var builder = new RestockingRedisConfig().restockingRedisHttp(properties).mutate();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new RestockingRedisClient(builder.build());
        server.expect(requestTo("http://redis-history:8001/v1/restockings/week?company_id=5&employee_id=72&limit=500"))
                .andExpect(header("X-Internal-API-Key", KEY)).andRespond(withSuccess("""
                        {"company_id":5,"employee_id":72,"week_start":"2026-10-19T00:00:00-03:00",
                         "week_end":"2026-10-26T00:00:00-03:00","total_cached":0,"items":[]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        var result = client.week(5, 72);
        assertThat(result.companyId()).isEqualTo(5);
        assertThat(result.weekStart().toInstant()).isEqualTo(Instant.parse("2026-10-19T03:00:00Z"));
        assertThat(result.items()).isEmpty();
        assertThatThrownBy(() -> client.week(5, 72)).isInstanceOf(org.springframework.web.client.RestClientException.class);
        server.verify();
    }

    @Test
    void validatesInternalConfigurationWithoutExposingTheKey() {
        assertThat(properties.toString()).doesNotContain(KEY);
        assertThatThrownBy(() -> new RestockingRedisProperties(true, URI.create("http://redis:8001"), "short",
                Duration.ofSeconds(1), Duration.ofSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RestockingRedisProperties(true, URI.create("http://user:secret@redis:8001"), KEY,
                Duration.ofSeconds(1), Duration.ofSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RestockingRedisProperties(true, URI.create("http://redis:8001"), KEY,
                Duration.ZERO, Duration.ofSeconds(1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void weekChangesAtMondayMidnightSaoPauloEvenWhenServerUsesUtc() {
        var before = RestockingWeek.current(Clock.fixed(Instant.parse("2026-10-19T02:59:59Z"), ZoneOffset.UTC));
        var after = RestockingWeek.current(Clock.fixed(Instant.parse("2026-10-19T03:00:00Z"), ZoneOffset.UTC));
        assertThat(before.start()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(after.start()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(before.endOffset()).isEqualTo(after.startOffset());
        assertThat(after.endOffset()).isEqualTo(OffsetDateTime.parse("2026-10-26T00:00:00-03:00"));
    }
}
