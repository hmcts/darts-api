package uk.gov.hmcts.darts.audio.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.darts.audio.component.AudioMessageDigest;
import uk.gov.hmcts.darts.audio.component.impl.AudioMessageDigestImpl;

@Configuration
@EnableConfigurationProperties(AudioConfigurationProperties.class)
public class AudioConfiguration {
    public static final String DEFAULT_ALGORITHM = "MD5";

    @Bean
    public AudioMessageDigest getMessageWrapper() {
        return new AudioMessageDigestImpl(DEFAULT_ALGORITHM);
    }
}