package uk.gov.hmcts.darts.authentication.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthConfiguration;
import uk.gov.hmcts.darts.authentication.config.external.ExternalProvider;
import uk.gov.hmcts.darts.common.util.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.security.AuthProvider;
import java.security.Provider;

@ConfigurationProperties("spring.security.oauth2.client.registration.external-azure-ad")
@Getter
public abstract class DefaultAuthConfiguration <T extends AuthProviderConfiguration> implements AuthConfiguration <T> {
    private String clientId;

    private String secret;

    private String scope;

    private String redirectURI;

    private String logoutRedirectURI;

    private String grantType;

    private String responseType;

    private String responseMode;

    private String prompt;

    private String issuerURI;

    private T provider;

    @Override
    public boolean doesMatch(HttpServletRequest req) {
        return RequestMatcher.URL_MAPPER_EXTERNAL.doesMatch(req);
    }
}
