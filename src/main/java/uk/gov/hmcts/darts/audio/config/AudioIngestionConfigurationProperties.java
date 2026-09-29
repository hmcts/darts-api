package uk.gov.hmcts.darts.audio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "darts.audio-ingestion")
@Getter
@Setter
public class AudioIngestionConfigurationProperties {

    private int batchSize = 5;

    private Duration processingTimeout = Duration.ofMinutes(15);

    private String temporaryFailureMode = "NONE";
}