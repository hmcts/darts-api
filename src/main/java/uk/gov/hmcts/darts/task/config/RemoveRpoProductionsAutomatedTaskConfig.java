package uk.gov.hmcts.darts.task.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@ConfigurationProperties("darts.automated.task.remove-old-arm-rpo-productions")
@Getter
@Setter
@Configuration
public class RemoveRpoProductionsAutomatedTaskConfig extends AbstractAutomatedTaskConfig {
    
    private Duration waitDuration;
    
}
