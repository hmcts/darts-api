package uk.gov.hmcts.darts.authentication.config;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import uk.gov.hmcts.darts.authentication.exception.AuthenticationException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static java.util.Objects.requireNonNullElse;

public interface AuthProviderConfigurationProperties {

    String getAuthorizationUri();

    String getTokenUri();

    String getJwkSetUri();

    String getLogoutUri();

    String getResetPasswordUri();

    default JWKSource<SecurityContext> getJwkSource() {
        try {
            URL jwksUrl = URI.create(requireNonNullElse(getJwkSetUri(), "")).toURL();

            return JWKSourceBuilder.create(jwksUrl).build();
        } catch (IllegalArgumentException | MalformedURLException exception) {
            throw new AuthenticationException("Sorry authentication jwks URL is incorrect", exception);
        }
    }
}
