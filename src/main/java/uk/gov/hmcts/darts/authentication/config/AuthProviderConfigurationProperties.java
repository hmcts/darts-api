package uk.gov.hmcts.darts.authentication.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import uk.gov.hmcts.darts.authentication.exception.AuthenticationException;

import java.net.MalformedURLException;
import java.net.URL;

public interface AuthProviderConfigurationProperties {

    String getAuthorizationURI();

    String getTokenURI();

    String getJwkSetUri();

    String getLogoutURI();

    String getResetPasswordURI();

    default JWKSource<SecurityContext> getJwkSource() {
        try {
            URL jwksUrl = new URL(getJwkSetUri());

            return new RemoteJWKSet<>(jwksUrl);
        }
        catch (MalformedURLException malformedURLException) {
            throw new AuthenticationException("Sorry authentication jwks URL is incorrect");
        }
    }
}
