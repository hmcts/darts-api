package uk.gov.hmcts.darts.authentication.client;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;

public interface OAuthClient {
    @SuppressWarnings({"PMD.UseObjectForClearerAPI"})
    HTTPResponse fetchAccessToken(AuthProviderConfigurationProperties providerConfigurationProperties,
                                  String redirectType, String authCode,
                                  String clientId,
                                  String authClientSecret, String scope);

    HTTPResponse fetchAccessToken(AuthProviderConfigurationProperties providerConfig,
                                  String refreshToken, String clientId,
                                  String clientSecret, String scope);
}
