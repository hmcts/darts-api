package uk.gov.hmcts.darts.task.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("darts.automated.task.process-dets-to-arm-response")
@Getter
@Setter
@Configuration
public class ProcessDetsToArmResponseAutomatedTaskConfig extends AbstractAutomatedTaskConfig {
}
