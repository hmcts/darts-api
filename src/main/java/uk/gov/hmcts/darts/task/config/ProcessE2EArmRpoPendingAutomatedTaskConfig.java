package uk.gov.hmcts.darts.task.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@ConfigurationProperties("darts.automated.task.process-e2e-arm-rpo-pending")
@Getter
@Setter
@Configuration
public class ProcessE2EArmRpoPendingAutomatedTaskConfig extends AbstractAutomatedTaskConfig {

    private boolean processE2eArmRpo;

    private Duration threadSleepDuration;
}
