package uk.gov.hmcts.darts.testutils.conf;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.darts.testutils.JwksWiremockInitialize;

@TestConfiguration
@Profile("tokenSecurityTest")
public class TokenConfiguration {

    @Bean
    public JwksWiremockInitialize getJwks() {
        return new JwksWiremockInitialize();
    }
}