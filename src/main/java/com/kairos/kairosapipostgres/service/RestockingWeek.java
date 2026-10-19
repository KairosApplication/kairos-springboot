package com.kairos.kairosapipostgres.service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

public record RestockingWeek(LocalDate start) {
    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    public static RestockingWeek current(Clock clock) {
        return containing(clock.instant().atZone(ZONE).toLocalDateTime());
    }

    public static RestockingWeek containing(LocalDateTime date) {
        return new RestockingWeek(date.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
    }

    public LocalDateTime from() {
        return start.atStartOfDay();
    }

    public LocalDateTime until() {
        return start.plusDays(7).atStartOfDay();
    }

    public OffsetDateTime startOffset() {
        return from().atZone(ZONE).toOffsetDateTime();
    }

    public OffsetDateTime endOffset() {
        return until().atZone(ZONE).toOffsetDateTime();
    }
}
