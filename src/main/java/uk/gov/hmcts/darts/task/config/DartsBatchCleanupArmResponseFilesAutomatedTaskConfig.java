package uk.gov.hmcts.darts.task.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("darts.automated.task.darts-batch-cleanup-arm-response-files")
@Getter
@Setter
@Configuration
public class DartsBatchCleanupArmResponseFilesAutomatedTaskConfig extends AbstractAutomatedTaskConfig {
}
