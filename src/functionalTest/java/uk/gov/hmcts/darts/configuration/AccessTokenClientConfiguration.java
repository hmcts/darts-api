package uk.gov.hmcts.darts.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.darts.AccessTokenClient;

@TestConfiguration
@EnableConfigurationProperties
@RequiredArgsConstructor
@Profile("functionalTest")
@Slf4j
public class AccessTokenClientConfiguration {

    private final AzureAdAuthenticationProperties adAuthenticationProperties;
    private final AzureAdB2CAuthenticationProperties b2cAuthenticationProperties;

    @Bean
    public AccessTokenClient internalAccessTokenClient() {
        log.info("internalAccessTokenClient {}", adAuthenticationProperties.getTokenUri());
        return new AccessTokenClient(adAuthenticationProperties.getTokenUri(),
                              adAuthenticationProperties.getScope(),
                              adAuthenticationProperties.getUsername(),
                              adAuthenticationProperties.getPassword(),
                              adAuthenticationProperties.getClientId(),
                              adAuthenticationProperties.getClientSecret());
    }

    @Bean
    public AccessTokenClient externalAccessTokenClient() {
        log.info("externalAccessTokenClient {}", b2cAuthenticationProperties.getTokenUri());
        return new AccessTokenClient(b2cAuthenticationProperties.getTokenUri(),
                                     b2cAuthenticationProperties.getScope(),
                                     b2cAuthenticationProperties.getUsername(),
                                     b2cAuthenticationProperties.getPassword(),
                                     b2cAuthenticationProperties.getClientId(),
                                     b2cAuthenticationProperties.getClientSecret());
    }

}
