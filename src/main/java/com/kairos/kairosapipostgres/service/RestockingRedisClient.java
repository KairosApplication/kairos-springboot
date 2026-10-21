package com.kairos.kairosapipostgres.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kairos.kairosapipostgres.dto.response.RestockingCacheEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class RestockingRedisClient {
    private final RestClient http;

    public RestockingRedisClient(@Qualifier("restockingRedisHttp") RestClient http) {
        this.http = http;
    }

    public void publish(RestockingCacheEvent event) {
        CacheResult result = http.post().uri("/v1/restockings/events").contentType(MediaType.APPLICATION_JSON)
                .body(event).retrieve().body(CacheResult.class);
        if (result == null || !(result.cached() || result.duplicate())) {
            throw new IllegalStateException("Redis API did not acknowledge the event");
        }
    }

    public CachedWeek week(long companyId, long employeeId) {
        return http.get().uri(builder -> builder.path("/v1/restockings/week")
                .queryParam("company_id", companyId).queryParam("employee_id", employeeId)
                .queryParam("limit", 500).build()).retrieve().body(CachedWeek.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CacheResult(boolean cached, boolean duplicate) { }

    public record CachedWeek(@JsonProperty("company_id") long companyId,
                              @JsonProperty("employee_id") long employeeId,
                              @JsonProperty("week_start") OffsetDateTime weekStart,
                              @JsonProperty("week_end") OffsetDateTime weekEnd,
                              @JsonProperty("total_cached") long totalCached,
                              List<RestockingCacheEvent> items) {
        public CachedWeek {
            if (items != null) {
                items = List.copyOf(items);
            }
        }

        public List<RestockingCacheEvent> items() {
            return items == null ? null : List.copyOf(items);
        }
    }
}
