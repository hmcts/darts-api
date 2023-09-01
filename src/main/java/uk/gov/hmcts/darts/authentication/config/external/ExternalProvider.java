package uk.gov.hmcts.darts.authentication.config.external;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfiguration;

import java.net.URI;

@ConfigurationProperties("spring.security.oauth2.client.provider.external-azure-ad-provider")
@Getter
public class ExternalProvider implements AuthProviderConfiguration {

    private String authorizationURI;

    private String tokenURI;

    private String JKWSURI;

    private String logoutURI;

    private String resetPasswordURI;
}
