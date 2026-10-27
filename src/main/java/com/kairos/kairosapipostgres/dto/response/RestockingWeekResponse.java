package com.kairos.kairosapipostgres.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record RestockingWeekResponse(long companyId, long employeeId, OffsetDateTime weekStart,
                                     OffsetDateTime weekEnd, long total, List<RestockingCacheEvent> items) {
    public RestockingWeekResponse {
        items = List.copyOf(items);
    }
}
