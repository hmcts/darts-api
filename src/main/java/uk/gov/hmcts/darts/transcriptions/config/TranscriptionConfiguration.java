package uk.gov.hmcts.darts.transcriptions.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TranscriptionConfigurationProperties.class)
public class TranscriptionConfiguration {

}
