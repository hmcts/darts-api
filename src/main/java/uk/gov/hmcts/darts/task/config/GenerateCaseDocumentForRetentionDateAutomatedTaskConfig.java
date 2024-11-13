package uk.gov.hmcts.darts.task.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("darts.automated.task.generate-case-document-for-retention-date")
@Getter
@Setter
@Configuration
public class GenerateCaseDocumentForRetentionDateAutomatedTaskConfig extends AbstractAutomatedTaskConfig {
}
