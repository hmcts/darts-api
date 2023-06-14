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
    public URI getAuthorizationUri() {
        URIBuilder uriBuilder = new URIBuilder(
            authConfig.getExternalADauthorizationUri());
        uriBuilder.addParameter("client_id", authConfig.getExternalADclientId());
        uriBuilder.addParameter("response_type", authConfig.getExternalADresponseType());
        uriBuilder.addParameter(
            "redirect_uri",
            authConfig.getExternalADredirectUri()
        );
        uriBuilder.addParameter("response_mode", authConfig.getExternalADresponseMode());
        uriBuilder.addParameter("scope", authConfig.getExternalADscope());
        uriBuilder.addParameter("prompt", authConfig.getExternalADprompt());
        return uriBuilder.build();
    }

    @Override
    public URI getLandingPageUri() {
        return URI.create("/");
    }

    @Override
    public URI getLogoutPageUri() {
        return URI.create("/");
    }

}
