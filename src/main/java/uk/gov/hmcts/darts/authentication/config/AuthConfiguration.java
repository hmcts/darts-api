package uk.gov.hmcts.darts.authentication.config;

import lombok.SneakyThrows;
import org.apache.http.client.utils.URIBuilder;
import uk.gov.hmcts.darts.common.util.RequestMatcher;

import java.net.URI;
import java.net.URISyntaxException;

public interface AuthConfiguration<T extends AuthProviderConfiguration> extends RequestMatcher {

    String getRedirectURI();

    String getLogoutRedirectURI();

    String getIssuerURI();

    String getPrompt();

    String getClientId();

    String getSecret();

    String getResponseMode();

    String getScope();

    String getGrantType();

    String getResponseType();

    T getProvider();

    @SneakyThrows(URISyntaxException.class)
    default URI getLoginUri(String redirectUri) {
        return buildCommonAuthUri(getProvider().getAuthorizationURI(), redirectUri)
            .addParameter("response_mode", getResponseMode())
            .addParameter("response_type", getResponseType())
            .build();
    }

    default URI getLandingPageUri() {
        return URI.create("/");
    }

    @SneakyThrows(URISyntaxException.class)
    default URI getLogoutUri(String accessToken, String redirectUriOverride) {
        var redirectUri = getLogoutRedirectURI();
        if (redirectUriOverride != null) {
            redirectUri = redirectUriOverride;
        }
        return new URIBuilder(
            getProvider().getLogoutURI())
            .addParameter("id_token_hint", accessToken)
            .addParameter("post_logout_redirect_uri", redirectUri)
            .build();
    }

    @SneakyThrows(URISyntaxException.class)
    default URI getResetPasswordUri(String redirectUri) {
        return buildCommonAuthUri(getProvider().getResetPasswordURI(), redirectUri)
            .addParameter("response_type", "id_token")
            .build();
    }

    @SneakyThrows(URISyntaxException.class)
    private URIBuilder buildCommonAuthUri(String uri, String redirectUriOverride) {
        var redirectUri = getRedirectURI();
        if (redirectUriOverride != null) {
            redirectUri = redirectUriOverride;
        }
        return new URIBuilder(uri)
            .addParameter("client_id", getClientId())
            .addParameter("redirect_uri", redirectUri)
            .addParameter("scope", getScope())
            .addParameter("prompt", getPrompt());
    }
}
