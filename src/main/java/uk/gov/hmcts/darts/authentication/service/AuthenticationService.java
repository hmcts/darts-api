package uk.gov.hmcts.darts.authentication.service;


import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;

public interface AuthenticationService {

    String getAuthorizationUrl();

    OAuthProviderRawResponse fetchAccessToken(String code);
}
