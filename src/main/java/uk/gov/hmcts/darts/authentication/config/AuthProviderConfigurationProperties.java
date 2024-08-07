package uk.gov.hmcts.darts.authentication.config;

import com.nimbusds.jose.jwk.source.DefaultJWKSetCache;
import com.nimbusds.jose.jwk.source.JWKSetCache;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import uk.gov.hmcts.darts.authentication.exception.AuthenticationException;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public interface AuthProviderConfigurationProperties {

    String getAuthorizationUri();

    String getTokenUri();

    String getJwkSetUri();

    String getLogoutUri();

    String getResetPasswordUri();

    @jakarta.validation.constraints.NotNull
    Duration getJwksCacheRefreshPeriod();

    @jakarta.validation.constraints.NotNull
    Duration getJwksCacheLifetimePeriod();

    default JWKSource<SecurityContext> getJwkSource() {
        try {
            URL jwksUrl = new URL(getJwkSetUri());

            return new RemoteJWKSet<>(jwksUrl, null, getJwkCache());
        } catch (MalformedURLException malformedUrlException) {
            throw new AuthenticationException("Sorry authentication jwks URL is incorrect", malformedUrlException);
        }
    }

    default JWKSetCache getJwkCache() {
        return new DefaultJWKSetCache(getJwksCacheLifetimePeriod().get(ChronoUnit.SECONDS),
                                      getJwksCacheRefreshPeriod().get(ChronoUnit.SECONDS),
                                      TimeUnit.SECONDS);
    }
}