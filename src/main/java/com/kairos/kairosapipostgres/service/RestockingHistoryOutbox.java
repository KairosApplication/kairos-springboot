package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.dto.response.RestockingCacheEvent;
import com.kairos.kairosapipostgres.model.*;
import com.kairos.kairosapipostgres.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.time.Clock;

/** Invoked inside the same PostgreSQL transaction as the restocking. */
@Service
public class RestockingHistoryOutbox {
    private final RestockingHistoryDeliveryRepository deliveries;
    private final RestockingHistoryBypassRepository bypasses;
    private final RestockingRepository restockings;
    private final ObjectMapper mapper;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RestockingHistoryOutbox(RestockingHistoryDeliveryRepository deliveries,
                                   RestockingHistoryBypassRepository bypasses, RestockingRepository restockings,
                                   ObjectMapper mapper, ApplicationEventPublisher events, Clock clock) {
        this.deliveries = deliveries;
        this.bypasses = bypasses;
        this.restockings = restockings;
        this.mapper = mapper;
        this.events = events;
        this.clock = clock;
    }

    public void enqueue(Restocking value, boolean afterCommit) {
        var week = RestockingWeek.containing(value.getRestockingDate());
        if (!week.equals(RestockingWeek.current(clock))) {
            return;
        }
        // Serialize concurrent cache repairs without relying on a check-then-insert race.
        restockings.locked(value.getId()).orElseThrow();
        var existing = deliveries.findFirstByRestockingId(value.getId());
        if (existing.isPresent() && !existing.get().isDelivered()) {
            return;
        }
        var delivery = existing.orElseGet(RestockingHistoryDelivery::new);
        var event = RestockingCacheEvent.from(value);
        delivery.setRestockingId(value.getId());
        delivery.setCompanyId(event.companyId());
        delivery.setEmployeeId(event.employeeId());
        delivery.setWeekStart(week.start());
        delivery.setPayload(mapper.writeValueAsString(event));
        delivery.setDelivered(false);
        delivery.setNextAttemptAt(clock.instant());
        delivery = deliveries.save(delivery);
        if (afterCommit) {
            events.publishEvent(new Ready(delivery.getId()));
        }
    }

    public void bypass(Restocking value) {
        var marker = new RestockingHistoryBypass();
        marker.setCompanyId(value.getEmployee().getCompany().getId());
        marker.setEmployeeId(value.getEmployee().getId());
        marker.setWeekStart(RestockingWeek.containing(value.getRestockingDate()).start());
        bypasses.save(marker);
        deliveries.deleteByRestockingId(value.getId());
    }

    public record Ready(Long id) { }
}
