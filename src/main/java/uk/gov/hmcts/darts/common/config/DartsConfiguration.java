package uk.gov.hmcts.darts.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.darts.audio.config.AudioTransformConfigurationProperties;

@Configuration
@ComponentScan({"uk.gov.hmcts.darts"})
@EnableConfigurationProperties(AudioTransformConfigurationProperties.class)
public class DartsConfiguration {

}
