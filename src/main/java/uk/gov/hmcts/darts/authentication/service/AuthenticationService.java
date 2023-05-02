package uk.gov.hmcts.darts.authentication.service;


import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;

import java.net.URI;

public interface AuthenticationService {

    URI loginOrRefresh(String sessionId);

    OAuthProviderRawResponse fetchAccessToken(String code);



}
