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
    public URI getLoginUri() {
        return buildCommonAuthUri(authConfig.getExternalADauthorizationUri())
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
    public URI getLogoutUri(String accessToken) {
        return new URIBuilder(
            authConfig.getExternalADlogoutUri())
            .addParameter("id_token_hint", accessToken)
            .addParameter("post_logout_redirect_uri", authConfig.getExternalADlogoutRedirectUri())
            .build();
    }

    @Override
    @SneakyThrows(URISyntaxException.class)
    public URI getResetPasswordUri() {
        return buildCommonAuthUri(authConfig.getExternalADresetPasswordUri())
            .addParameter("response_type", "id_token")
            .build();
    }

    @SneakyThrows(URISyntaxException.class)
    private URIBuilder buildCommonAuthUri(String uri) {
        return new URIBuilder(uri)
            .addParameter("client_id", authConfig.getExternalADclientId())
            .addParameter("redirect_uri", authConfig.getExternalADredirectUri())
            .addParameter("scope", authConfig.getExternalADscope())
            .addParameter("prompt", authConfig.getExternalADprompt());
    }

}
