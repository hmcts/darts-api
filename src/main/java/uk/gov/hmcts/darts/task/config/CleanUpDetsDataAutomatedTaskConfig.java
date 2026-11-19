package uk.gov.hmcts.darts.task.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@ConfigurationProperties("darts.automated.task.clean-up-dets-data")
@Getter
@Setter
@Configuration
public class CleanUpDetsDataAutomatedTaskConfig extends AbstractAsyncAutomatedTaskConfig {

    private Duration minimumStoredAge;
    private int chunkSize;
    @NotNull
    @DurationMin(seconds = 1)
    private Duration cleanupTimeout;
    @NotNull
    @DurationMin(seconds = 1)
    private Duration vacuumTimeout;
}
