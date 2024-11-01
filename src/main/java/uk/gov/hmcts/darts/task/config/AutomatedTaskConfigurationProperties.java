package uk.gov.hmcts.darts.task.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("darts.automated.task.common-config")
@Getter
@Setter
public class AutomatedTaskConfigurationProperties {

    private String systemUserEmail;
}
