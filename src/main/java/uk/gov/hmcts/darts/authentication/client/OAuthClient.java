package uk.gov.hmcts.darts.authentication.client;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;

public interface OAuthClient {
    HTTPResponse fetchAccessToken(AuthProviderConfigurationProperties providerConfigurationProperties,
                                  String redirectType, String authCode,
                                  String clientId,
                                  String authClientSecret, String scope);
}
