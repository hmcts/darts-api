package uk.gov.hmcts.darts.audio.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AudioConfigurationProperties.class)
public class AudioConfiguration {

}
