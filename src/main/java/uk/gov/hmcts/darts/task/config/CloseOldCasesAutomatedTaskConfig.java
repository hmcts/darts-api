package uk.gov.hmcts.darts.task.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@ConfigurationProperties("darts.automated.task.close-old-cases")
@Getter
@Setter
@Configuration
public class CloseOldCasesAutomatedTaskConfig extends AbstractAutomatedTaskConfig {

    private String closeEvents;
    private int threads;
    private Duration closeOpenCasesOlderThanDuration;
}
