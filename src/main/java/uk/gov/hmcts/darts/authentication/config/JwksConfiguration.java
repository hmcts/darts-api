package uk.gov.hmcts.darts.authentication.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URL;

@Configuration
@RequiredArgsConstructor
public class JwksConfiguration {

    private final AuthConfigurationLocator locator;

    @Bean
    public JWKSource<SecurityContext> jwkSource()
        throws MalformedURLException {
        URL jwksUrl = new URL(locator.locateAuthenticationConfigurationWithExternalDefault().getProvider().getJKWSURI());

        return new RemoteJWKSet<>(jwksUrl);
    }

}
