package uk.gov.hmcts.darts.audio.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AudioConfigurationProperties.class)
public class AudioConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

}
