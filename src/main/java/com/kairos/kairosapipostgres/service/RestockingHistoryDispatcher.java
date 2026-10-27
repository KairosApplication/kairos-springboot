package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.config.RestockingRedisProperties;
import com.kairos.kairosapipostgres.dto.response.RestockingCacheEvent;
import com.kairos.kairosapipostgres.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.Clock;

@Service
public class RestockingHistoryDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(RestockingHistoryDispatcher.class);
    private final RestockingHistoryDeliveryRepository deliveries;
    private final RestockingRedisClient client;
    private final RestockingRedisProperties properties;
    private final ObjectMapper mapper;
    private final Clock clock;

    public RestockingHistoryDispatcher(RestockingHistoryDeliveryRepository deliveries, RestockingRedisClient client,
                                       RestockingRedisProperties properties, ObjectMapper mapper, Clock clock) {
        this.deliveries = deliveries;
        this.client = client;
        this.properties = properties;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(Long id) {
        if (!properties.enabled()) {
            return;
        }
        var optional = deliveries.locked(id);
        if (optional.isEmpty() || optional.get().isDelivered()) {
            return;
        }
        var delivery = optional.get();
        if (!delivery.getWeekStart().equals(RestockingWeek.current(clock).start())) {
            delivery.setDelivered(true);
            return;
        }
        try {
            client.publish(mapper.readValue(delivery.getPayload(), RestockingCacheEvent.class));
            delivery.setDelivered(true);
        } catch (RuntimeException exception) {
            // Do not log API keys, HTTP headers, payloads or upstream response bodies.
            LOG.warn("Redis history event {} remains pending; retry scheduled", id);
            delivery.setNextAttemptAt(clock.instant().plusSeconds(30));
        }
    }
}
