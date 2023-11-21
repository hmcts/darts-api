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
    private final AzureAdB2CGlobalAuthenticationProperties b2cGlobalAuthenticationProperties;
    private final AzureAdB2CDarPcMidtierGlobalAuthenticationProperties b2CDarPcMidtierGlobalAuthenticationProperties;

    @Bean
    public AccessTokenClient internalAccessTokenClient() {
        log.info("adAuthenticationProperties username {}", adAuthenticationProperties.getUsername());
        return new AccessTokenClient(adAuthenticationProperties.getTokenUri(),
                              adAuthenticationProperties.getScope(),
                              adAuthenticationProperties.getUsername(),
                              adAuthenticationProperties.getPassword(),
                              adAuthenticationProperties.getClientId(),
                              adAuthenticationProperties.getClientSecret());
    }

    @Bean
    public AccessTokenClient externalAccessTokenClient() {
        log.info("b2cAuthenticationProperties username {}", b2cAuthenticationProperties.getUsername());
        return new AccessTokenClient(b2cAuthenticationProperties.getTokenUri(),
                                     b2cAuthenticationProperties.getScope(),
                                     b2cAuthenticationProperties.getUsername(),
                                     b2cAuthenticationProperties.getPassword(),
                                     b2cAuthenticationProperties.getClientId(),
                                     b2cAuthenticationProperties.getClientSecret());
    }

    @Bean
    public AccessTokenClient externalGlobalAccessTokenClient() {
        log.info("externalGlobalAccessTokenClient username {}", b2cGlobalAuthenticationProperties.getUsername());
        return new AccessTokenClient(b2cGlobalAuthenticationProperties.getTokenUri(),
                                     b2cGlobalAuthenticationProperties.getScope(),
                                     b2cGlobalAuthenticationProperties.getUsername(),
                                     b2cGlobalAuthenticationProperties.getPassword(),
                                     b2cGlobalAuthenticationProperties.getClientId(),
                                     b2cGlobalAuthenticationProperties.getClientSecret());
    }

    @Bean
    public AccessTokenClient externalDarPcMidTierGlobalAccessTokenClient() {
        log.info("b2CDarPcMidtierGlobalAuthenticationProperties username {}", b2CDarPcMidtierGlobalAuthenticationProperties.getUsername());
        return new AccessTokenClient(b2CDarPcMidtierGlobalAuthenticationProperties.getTokenUri(),
                                     b2CDarPcMidtierGlobalAuthenticationProperties.getScope(),
                                     b2CDarPcMidtierGlobalAuthenticationProperties.getUsername(),
                                     b2CDarPcMidtierGlobalAuthenticationProperties.getPassword(),
                                     b2CDarPcMidtierGlobalAuthenticationProperties.getClientId(),
                                     b2CDarPcMidtierGlobalAuthenticationProperties.getClientSecret());
    }

}
