package uk.gov.hmcts.darts.authentication.service;

import java.net.URI;

public interface AuthenticationService {

    URI loginOrRefresh(String accessToken);

    String handleOauthCode(String code);

    URI logout(String accessToken);

}
