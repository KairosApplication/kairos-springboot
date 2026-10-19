package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.config.RestockingRedisProperties;
import com.kairos.kairosapipostgres.repository.*;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import java.time.Clock;

@Service
public class RestockingHistoryWorker {
    private final RestockingHistoryDeliveryRepository deliveries;
    private final RestockingHistoryBypassRepository bypasses;
    private final RestockingHistoryDispatcher dispatcher;
    private final RestockingRedisProperties properties;
    private final Clock clock;

    public RestockingHistoryWorker(RestockingHistoryDeliveryRepository deliveries,
                                   RestockingHistoryBypassRepository bypasses, RestockingHistoryDispatcher dispatcher,
                                   RestockingRedisProperties properties, Clock clock) {
        this.deliveries = deliveries;
        this.bypasses = bypasses;
        this.dispatcher = dispatcher;
        this.properties = properties;
        this.clock = clock;
    }

    @TransactionalEventListener
    public void committed(RestockingHistoryOutbox.Ready event) {
        safelyDeliver(event.id());
    }

    @Scheduled(fixedDelayString = "${app.restocking-redis.retry-delay:5000}",
            initialDelayString = "${app.restocking-redis.retry-delay:5000}")
    @Transactional
    public void retry() {
        if (!properties.enabled()) {
            return;
        }
        deliveries.findByDeliveredFalseAndNextAttemptAtLessThanEqualOrderById(clock.instant(), PageRequest.of(0, 25))
                .forEach(delivery -> safelyDeliver(delivery.getId()));
    }

    @Scheduled(fixedDelay = 3600000, initialDelay = 3600000)
    @Transactional
    public void cleanup() {
        var week = RestockingWeek.current(clock).start();
        deliveries.deleteByWeekStartBefore(week);
        bypasses.deleteByWeekStartBefore(week);
    }

    private void safelyDeliver(Long id) {
        try {
            dispatcher.deliver(id);
        } catch (RuntimeException exception) {
            // A committed restocking must never appear to fail because a cache notification failed.
            LoggerFactory.getLogger(RestockingHistoryWorker.class).warn("Redis history delivery {} will be retried", id);
        }
    }
}
