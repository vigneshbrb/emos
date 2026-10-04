package com.emos.attentionfollowthrough.domain;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

public final class WorkingCalendar {
    private final ZoneId zone;
    private final Set<LocalDate> holidays;

    public WorkingCalendar(ZoneId zone, Set<LocalDate> holidays) {
        if (zone == null) throw new IllegalArgumentException("Zone is required");
        this.zone = zone;
        this.holidays = Set.copyOf(holidays);
    }

    public Instant addWorkingHours(Instant start, Duration amount) {
        if (amount.isNegative()) throw new IllegalArgumentException("Working duration cannot be negative");
        var cursor = start;
        var remaining = amount;
        while (!remaining.isZero()) {
            var local = cursor.atZone(zone);
            var nextDay = local.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant();
            if (!isWorking(local.toLocalDate())) {
                cursor = nextDay;
                continue;
            }
            var available = Duration.between(cursor, nextDay);
            if (remaining.compareTo(available) <= 0) return cursor.plus(remaining);
            remaining = remaining.minus(available);
            cursor = nextDay;
        }
        return cursor;
    }

    private boolean isWorking(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY
                && !holidays.contains(date);
    }
}
