package uk.gov.hmcts.darts.audio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AudioConfigurationProperties.class)
public class AudioConfiguration {

    @Value("${darts.audio.outbounddeleter.last-accessed-deletion-day:2}")
    private long lastAccessedDeletionDays;

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}
