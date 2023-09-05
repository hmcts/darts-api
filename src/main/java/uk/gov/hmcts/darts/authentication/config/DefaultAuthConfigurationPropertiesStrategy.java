package uk.gov.hmcts.darts.authentication.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthProviderConfigurationProperties;

@Component
@RequiredArgsConstructor
public class DefaultAuthConfigurationPropertiesStrategy implements AuthenticationConfigurationPropertiesStrategy {
    private final ExternalAuthConfigurationProperties configuration;

    private final ExternalAuthProviderConfigurationProperties provider;

    @Override
    public AuthConfigurationProperties getConfiguration() {
        return configuration;
    }

    @Override
    public AuthProviderConfigurationProperties getProviderConfiguration() {
        return provider;
    }

    @Override
    public boolean doesMatch(HttpServletRequest req) {
        return false;
    }
}
