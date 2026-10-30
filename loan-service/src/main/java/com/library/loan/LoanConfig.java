package com.library.loan;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class LoanConfig {

    /** Due dates are calendar days in the library's time zone. */
    @Bean
    Clock clock(@Value("${library.time-zone:Europe/Istanbul}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
