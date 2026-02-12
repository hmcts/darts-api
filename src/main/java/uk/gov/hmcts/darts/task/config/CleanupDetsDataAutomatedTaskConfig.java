package uk.gov.hmcts.darts.task.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@ConfigurationProperties("darts.automated.task.cleanup-dets-data")
@Getter
@Setter
@Configuration
public class CleanupDetsDataAutomatedTaskConfig extends AbstractAutomatedTaskConfig {

    private Duration durationInArmStorage;
    private Integer partitionSize;

}
