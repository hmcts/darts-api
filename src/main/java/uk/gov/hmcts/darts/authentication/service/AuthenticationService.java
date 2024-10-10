package uk.gov.hmcts.darts.authentication.service;

import java.net.URI;

public interface AuthenticationService {

    URI loginOrRefresh(String accessToken, String redirectUri);

    String handleOauthCode(String code, String redirectUri);

    URI logout(String accessToken, String redirectUri);

    URI resetPassword(String redirectUri);

}
