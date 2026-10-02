package uk.gov.hmcts.darts.audio.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AudioIngestionConfigurationPropertiesTest {

    @Test
    void shouldHaveExpectedDefaults() {
        AudioIngestionConfigurationProperties properties =
            new AudioIngestionConfigurationProperties();

        assertThat(properties.getBatchSize()).isEqualTo(5);
        assertThat(properties.getProcessingTimeout())
            .isEqualTo(Duration.ofMinutes(15));
        assertThat(properties.getTemporaryFailureMode())
            .isEqualTo("NONE");
    }
}