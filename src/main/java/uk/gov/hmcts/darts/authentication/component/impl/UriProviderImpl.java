package uk.gov.hmcts.darts.authentication.component.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authentication.component.UriProvider;
import uk.gov.hmcts.darts.authentication.config.AuthenticationConfiguration;

import java.net.URI;
import java.net.URISyntaxException;

@Component
@RequiredArgsConstructor
public class UriProviderImpl implements UriProvider {

    private final AuthenticationConfiguration authConfig;

    @Override
    @SneakyThrows(URISyntaxException.class)
    public URI getLoginUri(String redirectUri) {
        return buildCommonAuthUri(authConfig.getExternalADauthorizationUri(), redirectUri)
            .addParameter("response_mode", authConfig.getExternalADresponseMode())
            .addParameter("response_type", authConfig.getExternalADresponseType())
            .build();
    }

    @Override
    public URI getLandingPageUri() {
        return URI.create("/");
    }

    @Override
    @SneakyThrows(URISyntaxException.class)
    public URI getLogoutUri(String accessToken, String redirectUriOverride) {
        var redirectUri = authConfig.getExternalADlogoutRedirectUri();
        if (redirectUriOverride != null) {
            redirectUri = redirectUriOverride;
        }
        return new URIBuilder(
            authConfig.getExternalADlogoutUri())
            .addParameter("id_token_hint", accessToken)
            .addParameter("post_logout_redirect_uri", redirectUri)
            .build();
    }

    @Override
    @SneakyThrows(URISyntaxException.class)
    public URI getResetPasswordUri(String redirectUri) {
        return buildCommonAuthUri(authConfig.getExternalADresetPasswordUri(), redirectUri)
            .addParameter("response_type", "id_token")
            .build();
    }

    @SneakyThrows(URISyntaxException.class)
    private URIBuilder buildCommonAuthUri(String uri, String redirectUriOverride) {
        var redirectUri = authConfig.getExternalADredirectUri();
        if (redirectUriOverride != null) {
            redirectUri = redirectUriOverride;
        }
        return new URIBuilder(uri)
            .addParameter("client_id", authConfig.getExternalADclientId())
            .addParameter("redirect_uri", redirectUri)
            .addParameter("scope", authConfig.getExternalADscope())
            .addParameter("prompt", authConfig.getExternalADprompt());
    }

}
