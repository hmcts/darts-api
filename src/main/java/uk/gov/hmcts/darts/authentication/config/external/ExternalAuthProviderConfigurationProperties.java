package uk.gov.hmcts.darts.authentication.config.external;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;

@Component
@ConfigurationProperties("spring.security.oauth2.client.provider.external-azure-ad-provider")
@Getter
@Setter
@EqualsAndHashCode
public class ExternalAuthProviderConfigurationProperties implements AuthProviderConfigurationProperties {

    private String authorizationUri;

    private String tokenUri;

    private String jwkSetUri;

    private String logoutUri;

    private String resetPasswordUri;

    @Override
    @Cacheable(value = "external_jwk_source")
    public JWKSource<SecurityContext> getJwkSource() {
        return AuthProviderConfigurationProperties.super.getJwkSource();
    }
}