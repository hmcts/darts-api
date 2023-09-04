package uk.gov.hmcts.darts.authentication.config.internal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.darts.authentication.config.AuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.AuthenticationConfigurationPropertiesStrategy;
import uk.gov.hmcts.darts.common.util.RequestMatcher;

@Configuration
@RequiredArgsConstructor
public class InternalAuthConfigurationPropertiesStrategy implements AuthenticationConfigurationPropertiesStrategy {
    private final InternalAuthConfigurationProperties configuration;

    private final InternalAuthProviderConfigurationProperties provider;

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
        return RequestMatcher.URL_MAPPER_INTERNAL.doesMatch(req);
    }
}
