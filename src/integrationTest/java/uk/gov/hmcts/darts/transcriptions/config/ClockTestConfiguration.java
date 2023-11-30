package uk.gov.hmcts.darts.transcriptions.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
@ConfigurationProperties("test.clock")
public class ClockTestConfiguration {

    @Value("${test.clock.fixed-instant:2023-11-24T14:30:00Z}")
    private String fixedInstant;

    @Bean("testClockFixed")
    @Primary
    public Clock clock() {
        return Clock.fixed(Instant.parse(fixedInstant), ZoneId.of("UTC"));
    }

}
