package uk.gov.hmcts.darts.authentication.client.impl;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
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
    @SuppressWarnings({"PMD.LooseCoupling", "PMD.UseObjectForClearerAPI"})
    @Override
    public HTTPResponse fetchAccessToken(AuthProviderConfigurationProperties providerConfigurationProperties,
                                         String redirectType, String authCode,
                                         String clientId,
                                         String authClientSecret,
                                         String scope) {
        AuthorizationCode code = new AuthorizationCode(authCode);
        URI callback = new URI(redirectType);
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(code, callback);
        Scope authScope = new Scope();
        authScope.add(scope);
        authScope.add(OIDCScopeValue.PROFILE);
        authScope.add(OIDCScopeValue.EMAIL);
        authScope.add(OIDCScopeValue.OFFLINE_ACCESS);

        ClientID clientID = new ClientID(clientId);
        Secret clientSecret = new Secret(authClientSecret);
        ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

        URI tokenEndpoint = new URI(providerConfigurationProperties.getTokenUri());

        TokenRequest request = new TokenRequest(tokenEndpoint, clientAuth, codeGrant, authScope);
        return request.toHTTPRequest().send();
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    @SuppressWarnings({"PMD.UseObjectForClearerAPI"})
    @Override
    public HTTPResponse fetchAccessToken(AuthProviderConfigurationProperties providerConfigurationProperties,
                                         String refreshTokenParam,
                                         String clientId,
                                         String authClientSecret,
                                         String scope) {

        // Construct the grant from the saved refresh token
        RefreshToken refreshToken = new RefreshToken(refreshTokenParam);
        AuthorizationGrant refreshTokenGrant = new RefreshTokenGrant(refreshToken);

        // The credentials to authenticate the client at the token endpoint
        ClientID clientID = new ClientID(clientId);
        Secret clientSecret = new Secret(authClientSecret);
        ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

        // The token endpoint
        URI tokenEndpoint = new URI(providerConfigurationProperties.getTokenUri());

        // Make the token request
        TokenRequest request = new TokenRequest(tokenEndpoint, clientAuth, refreshTokenGrant);

        return request.toHTTPRequest().send();
    }
}
