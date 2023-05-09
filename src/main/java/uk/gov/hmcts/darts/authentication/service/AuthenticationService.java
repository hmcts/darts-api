package uk.gov.hmcts.darts.authentication.service;


import java.net.URI;

public interface AuthenticationService {

    URI loginOrRefresh(String sessionId);

    URI handleOauthCode(String sessionId, String code);

}
