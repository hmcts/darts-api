package uk.gov.hmcts.darts.authentication.config.external;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authentication.config.AuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.AuthenticationConfigurationPropertiesStrategy;
import uk.gov.hmcts.darts.common.util.RequestMatcher;

@Component
@RequiredArgsConstructor
public class ExternalAuthConfigurationPropertiesStrategy implements AuthenticationConfigurationPropertiesStrategy {

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
        return RequestMatcher.URL_MAPPER_EXTERNAL.doesMatch(req);
    }
}
