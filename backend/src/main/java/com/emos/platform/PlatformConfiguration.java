package com.emos.platform;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class PlatformConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
