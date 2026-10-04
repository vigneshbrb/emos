package com.emos.attentionfollowthrough.infrastructure;

import com.emos.attentionfollowthrough.domain.WorkingCalendar;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
class AttentionConfiguration {
    @Bean
    WorkingCalendar workingCalendar(@Value("${emos.calendar.zone:Asia/Kolkata}") String zone,
                                    @Value("${emos.calendar.holidays:}") String holidays) {
        Set<LocalDate> dates = holidays.isBlank() ? Set.of() : Arrays.stream(holidays.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).map(LocalDate::parse)
                .collect(Collectors.toUnmodifiableSet());
        return new WorkingCalendar(ZoneId.of(zone), dates);
    }
}
