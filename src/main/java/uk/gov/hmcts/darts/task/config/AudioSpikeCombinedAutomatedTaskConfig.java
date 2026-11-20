package uk.gov.hmcts.darts.task.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("darts.automated.task.audio-spike-combined")
public class AudioSpikeCombinedAutomatedTaskConfig extends AbstractAutomatedTaskConfig {
}
