package uk.gov.hmcts.darts.audio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("darts.audio.audio-transformation-service")
@Getter
@Setter
public class AudioTransformationServiceProperties {

    private Integer loopCutoffMinutes;
}
