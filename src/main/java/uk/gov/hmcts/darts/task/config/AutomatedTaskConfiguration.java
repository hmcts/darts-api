package uk.gov.hmcts.darts.task.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AutomatedTaskConfigurationProperties.class)
public class AutomatedTaskConfiguration {

}
