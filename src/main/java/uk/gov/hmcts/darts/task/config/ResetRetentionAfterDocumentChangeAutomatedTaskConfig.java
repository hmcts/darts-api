package uk.gov.hmcts.darts.task.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("darts.automated.task.reset-retention-after-document-change")
@Getter
@Setter
@Configuration
public class ResetRetentionAfterDocumentChangeAutomatedTaskConfig extends AbstractAutomatedTaskConfig {

}
