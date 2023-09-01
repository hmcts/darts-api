package uk.gov.hmcts.darts.authentication.config.internal;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.DefaultAuthConfiguration;
import uk.gov.hmcts.darts.common.util.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

@ConfigurationProperties("spring.security.oauth2.client.registration.internal-azure-ad")
@Getter
public class InternalAuthConfiguration extends DefaultAuthConfiguration<uk.gov.hmcts.darts.authentication.config.internal.InternalProvider> {

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

    private InternalProvider provider;

    @Override
    public boolean doesMatch(HttpServletRequest req) {
        return RequestMatcher.URL_MAPPER_INTERNAL.doesMatch(req);
    }
}
