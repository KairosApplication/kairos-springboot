package com.kairos.kairosapipostgres.service;

import com.kairos.kairosapipostgres.config.RestockingRedisProperties;
import com.kairos.kairosapipostgres.dto.response.*;
import com.kairos.kairosapipostgres.exception.InvalidRestockingException;
import com.kairos.kairosapipostgres.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.*;

@Service
public class RestockingWeeklyHistory {
    private final RestockingRepository restockings;
    private final RestockingHistoryDeliveryRepository deliveries;
    private final RestockingHistoryBypassRepository bypasses;
    private final RestockingAccess access;
    private final RestockingHistoryOutbox outbox;
    private final RestockingRedisClient client;
    private final RestockingRedisProperties properties;
    private final Clock clock;

    public RestockingWeeklyHistory(RestockingRepository restockings, RestockingHistoryDeliveryRepository deliveries,
                                   RestockingHistoryBypassRepository bypasses, RestockingAccess access,
                                   RestockingHistoryOutbox outbox, RestockingRedisClient client,
                                   RestockingRedisProperties properties, Clock clock) {
        this.restockings = restockings;
        this.deliveries = deliveries;
        this.bypasses = bypasses;
        this.access = access;
        this.outbox = outbox;
        this.client = client;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public RestockingWeekResponse week(Long employeeId, int limit) {
        if (limit < 1 || limit > 500 || (employeeId != null && employeeId <= 0)) {
            throw new InvalidRestockingException("limit must be between 1 and 500; employeeId must be positive");
        }
        var target = access.target(employeeId);
        long company = target.getCompany().getId();
        long employee = target.getId();
        var week = RestockingWeek.current(clock);
        long total = restockings.countWeek(company, employee, week.from(), week.until());
        boolean bypass = bypasses.existsByCompanyIdAndEmployeeIdAndWeekStart(company, employee, week.start());
        boolean pending = deliveries.existsByCompanyIdAndEmployeeIdAndWeekStartAndDeliveredFalse(company, employee, week.start());
        List<RestockingCacheEvent> items = null;
        if (properties.enabled() && !bypass && !pending && total > 0 && total <= 500) {
            items = cached(company, employee, week, total);
        }
        if (items == null) {
            boolean repair = properties.enabled() && !bypass && !pending && total <= 500;
            var rows = restockings.week(company, employee, week.from(), week.until(), PageRequest.of(0, repair ? 500 : limit));
            if (repair) {
                rows.forEach(row -> outbox.enqueue(row, false));
            }
            items = rows.stream().map(RestockingCacheEvent::from).toList();
        }
        return new RestockingWeekResponse(company, employee, week.startOffset(), week.endOffset(), total,
                items.stream().sorted(Comparator.comparing(RestockingCacheEvent::restockingDate).reversed()
                        .thenComparing(Comparator.comparingLong(RestockingCacheEvent::restockingId).reversed()))
                        .limit(limit).toList());
    }

    private List<RestockingCacheEvent> cached(long company, long employee, RestockingWeek week, long total) {
        try {
            var value = client.week(company, employee);
            if (value == null) {
                return null;
            }
            var start = value.weekStart();
            var end = value.weekEnd();
            var cachedItems = value.items();
            if (value.companyId() != company || value.employeeId() != employee || start == null || end == null
                    || !start.isEqual(week.startOffset()) || !end.isEqual(week.endOffset())
                    || value.totalCached() != total || cachedItems == null || cachedItems.size() != total) {
                return null;
            }
            Set<Long> ids = new HashSet<>();
            for (var item : cachedItems) {
                if (item == null || item.companyId() != company || item.employeeId() != employee
                        || item.restockingDate() == null || !RestockingWeek.containing(item.restockingDate()
                            .atZoneSameInstant(RestockingWeek.ZONE).toLocalDateTime()).equals(week)
                        || !ids.add(item.restockingId())) {
                    return null;
                }
            }
            return cachedItems;
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
