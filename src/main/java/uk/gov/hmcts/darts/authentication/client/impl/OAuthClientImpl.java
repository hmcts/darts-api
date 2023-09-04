package uk.gov.hmcts.darts.authentication.client.impl;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authentication.client.OAuthClient;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


@Component
public class OAuthClientImpl implements OAuthClient {
    @SneakyThrows({URISyntaxException.class, IOException.class})
    public HTTPResponse fetchAccessToken(AuthProviderConfigurationProperties providerConfigurationProperties,
                                         String redirectType, String authCode,
                                         String clientId,
                                         String authClientSecret) {
        AuthorizationCode code = new AuthorizationCode(authCode);
        URI callback = new URI(redirectType);
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, callback);

        ClientID clientID = new ClientID(clientId);
        Secret clientSecret = new Secret(authClientSecret);
        ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

        URI tokenEndpoint = new URI(providerConfigurationProperties.getTokenUri());

        TokenRequest request = new TokenRequest(tokenEndpoint, clientAuth, codeGrant);
        HTTPResponse response = request.toHTTPRequest().send();
        return response;
    }
}
