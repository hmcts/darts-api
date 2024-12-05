package uk.gov.hmcts.darts.authentication.service;

import uk.gov.hmcts.darts.authentication.model.TokenResponse;

import java.net.URI;

public interface AuthenticationService {

    URI loginOrRefresh(String accessToken, String redirectUri);

    TokenResponse handleOauthCode(String code, String redirectUri);

    String refreshAccessToken(String refreshToken);

    URI logout(String accessToken, String redirectUri);

    URI resetPassword(String redirectUri);

}
