package uk.gov.hmcts.darts.task.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("darts.automated.task.apply-retention-case-associated-objects")
@Getter
@Setter
@Configuration
public class ApplyRetentionCaseAssociatedObjectsAutomatedTaskConfig extends AbstractAutomatedTaskConfig {
}
