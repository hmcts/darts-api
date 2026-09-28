package uk.gov.hmcts.darts.audio.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties("darts.audio-ingestion")
@Getter
@Setter
@ToString
@Validated
public class AudioIngestionConfigurationProperties {

    private int batchSize = 5;

    private Duration processingTimeout = Duration.ofMinutes(15);

    /*
     * Spike-only configuration used to prove temporary failure/retry.
     */
    private TemporaryFailureMode temporaryFailureMode = TemporaryFailureMode.NONE;

    public enum TemporaryFailureMode {
        NONE,
        ALL,
        PARTIAL
    }
}
