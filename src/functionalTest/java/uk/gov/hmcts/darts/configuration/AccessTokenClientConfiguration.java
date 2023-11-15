package uk.gov.hmcts.darts.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.darts.AccessTokenClient;

@TestConfiguration
@EnableConfigurationProperties
@RequiredArgsConstructor
@Profile("functionalTest")
public class AccessTokenClientConfiguration {

    private final AzureAdAuthenticationProperties adAuthenticationProperties;
    private final AzureAdB2CAuthenticationProperties b2cAuthenticationProperties;

    @Bean
    public AccessTokenClient internalAccessTokenClient() {
        return new AccessTokenClient(adAuthenticationProperties.getTokenUri(),
                              adAuthenticationProperties.getScope(),
                              adAuthenticationProperties.getUsername(),
                              adAuthenticationProperties.getPassword(),
                              adAuthenticationProperties.getClientId(),
                              adAuthenticationProperties.getClientSecret());
    }

    @Bean
    public AccessTokenClient externalAccessTokenClient() {
        return new AccessTokenClient(b2cAuthenticationProperties.getTokenUri(),
                                     b2cAuthenticationProperties.getScope(),
                                     b2cAuthenticationProperties.getUsername(),
                                     b2cAuthenticationProperties.getPassword(),
                                     b2cAuthenticationProperties.getClientId(),
                                     b2cAuthenticationProperties.getClientSecret());
    }

    @Bean
    public AccessTokenClient externalGlobalAccessTokenClient() {
        return new AccessTokenClient(b2cAuthenticationProperties.getTokenUri(),
                                     b2cAuthenticationProperties.getScope(),
                                     b2cAuthenticationProperties.getGlobalUsername(),
                                     b2cAuthenticationProperties.getGlobalPassword(),
                                     b2cAuthenticationProperties.getClientId(),
                                     b2cAuthenticationProperties.getClientSecret());
    }

}
