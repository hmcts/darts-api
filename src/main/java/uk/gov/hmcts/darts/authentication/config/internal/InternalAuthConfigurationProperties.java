package uk.gov.hmcts.darts.authentication.config.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authentication.config.AuthConfigurationProperties;

@Component
@ConfigurationProperties("spring.security.oauth2.client.registration.internal-azure-ad")
@Getter
@Setter
public class InternalAuthConfigurationProperties implements AuthConfigurationProperties {

    private String clientId;

    private String clientSecret;

    private String scope;

    private String redirectURI;

    private String logoutRedirectURI;

    private String grantType;

    private String responseType;

    private String responseMode;

    private String prompt;

    private String issuerURI;
}
