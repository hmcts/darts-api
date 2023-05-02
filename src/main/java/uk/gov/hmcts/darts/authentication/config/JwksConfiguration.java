package uk.gov.hmcts.darts.authentication.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URL;

@Configuration
public class JwksConfiguration {

    @Bean
    public JWKSource<SecurityContext> jwkSource(AuthenticationConfiguration authenticationConfiguration)
        throws MalformedURLException {
        URL jwksUrl = new URL(authenticationConfiguration.getExternalADjwkSetUri());

        return new RemoteJWKSet<>(jwksUrl);
    }

}
