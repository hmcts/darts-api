package uk.gov.hmcts.darts.authentication.config.external;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;

import java.time.Duration;

@Component
@ConfigurationProperties("spring.security.oauth2.client.provider.external-azure-ad-provider")
@Getter
@Setter
@Validated
public class ExternalAuthProviderConfigurationProperties implements AuthProviderConfigurationProperties {

    private String authorizationUri;

    private String tokenUri;

    private String jwkSetUri;

    private String logoutUri;

    private String resetPasswordUri;

    private Duration jwksCacheRefreshPeriod;

    private Duration jwksCacheLifetimePeriod;
}