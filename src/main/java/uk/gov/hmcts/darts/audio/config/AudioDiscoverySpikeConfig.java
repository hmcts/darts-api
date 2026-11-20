package uk.gov.hmcts.darts.audio.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties("darts.audio-discovery-spike")
public class AudioDiscoverySpikeConfig {
    private String connectionString;
    private String cvpContainer;
    private String carContainer;
    private String prefix;
}
