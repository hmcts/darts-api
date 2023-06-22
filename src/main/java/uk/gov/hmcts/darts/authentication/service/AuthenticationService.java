package uk.gov.hmcts.darts.authentication.service;


import java.net.URI;

public interface AuthenticationService {

    URI loginOrRefresh(String sessionId);

    String handleOauthCode(String sessionId, String code);

    URI logout(String sessionId);

    void invalidateSession(String sessionId);

}
