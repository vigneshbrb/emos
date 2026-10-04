package com.emos.attentionfollowthrough.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WorkingCalendarPropertyTest {
    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    @ParameterizedTest
    @ValueSource(strings = {
            "2026-10-05T00:00:00Z", "2026-10-06T08:31:00Z", "2026-10-09T18:29:59Z",
            "2026-10-10T03:00:00Z", "2026-10-11T20:00:00Z", "2026-10-12T02:00:00Z"
    })
    void result_is_working_time_and_exactly_twenty_four_working_hours_were_counted(String input) {
        var holiday = LocalDate.parse("2026-10-12");
        var calendar = new WorkingCalendar(INDIA, Set.of(holiday));
        var start = Instant.parse(input);

        var result = calendar.addWorkingHours(start, Duration.ofHours(24));

        var localResult = result.atZone(INDIA);
        assertThat(localResult.getDayOfWeek()).isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        assertThat(localResult.toLocalDate()).isNotEqualTo(holiday);
        assertThat(countWorkingSeconds(start, result, holiday)).isEqualTo(86_400);
    }

    private long countWorkingSeconds(Instant start, Instant end, LocalDate holiday) {
        long seconds = 0;
        for (var cursor = start; cursor.isBefore(end); cursor = cursor.plusSeconds(1)) {
            var local = cursor.atZone(INDIA);
            if (local.getDayOfWeek() != DayOfWeek.SATURDAY
                    && local.getDayOfWeek() != DayOfWeek.SUNDAY
                    && !local.toLocalDate().equals(holiday)) seconds++;
        }
        return seconds;
    }
}
