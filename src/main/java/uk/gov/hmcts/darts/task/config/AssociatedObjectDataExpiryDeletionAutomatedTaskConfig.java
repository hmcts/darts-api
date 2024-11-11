package uk.gov.hmcts.darts.task.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("darts.automated.task.associated-object-data-expiry-deletion")
@Getter
@Setter
@Configuration
public class AssociatedObjectDataExpiryDeletionAutomatedTaskConfig extends AbstractAutomatedTaskConfig {
}
