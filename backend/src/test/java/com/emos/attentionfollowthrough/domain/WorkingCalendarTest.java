package com.emos.attentionfollowthrough.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorkingCalendarTest {
  private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

  @Test
  void counts_every_hour_on_weekdays() {
    var calendar = new WorkingCalendar(INDIA, Set.of());

    assertThat(calendar.addWorkingHours(at("2026-10-05T10:00:00+05:30"), Duration.ofHours(24)))
        .isEqualTo(at("2026-10-06T10:00:00+05:30"));
  }

  @Test
  void pauses_during_weekends() {
    var calendar = new WorkingCalendar(INDIA, Set.of());

    assertThat(calendar.addWorkingHours(at("2026-10-09T10:00:00+05:30"), Duration.ofHours(24)))
        .isEqualTo(at("2026-10-12T10:00:00+05:30"));
  }

  @Test
  void pauses_during_explicit_whole_day_holidays() {
    var calendar = new WorkingCalendar(INDIA, Set.of(LocalDate.parse("2026-10-12")));

    assertThat(calendar.addWorkingHours(at("2026-10-09T10:00:00+05:30"), Duration.ofHours(24)))
        .isEqualTo(at("2026-10-13T10:00:00+05:30"));
  }

  private static Instant at(String value) {
    return java.time.OffsetDateTime.parse(value).toInstant();
  }
}
