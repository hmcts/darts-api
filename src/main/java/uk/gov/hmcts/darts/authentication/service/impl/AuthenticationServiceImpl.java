package uk.gov.hmcts.darts.authentication.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authentication.component.TokenValidator;
import uk.gov.hmcts.darts.authentication.component.UriProvider;
import uk.gov.hmcts.darts.authentication.dao.AzureDao;
import uk.gov.hmcts.darts.authentication.exception.AuthenticationException;
import uk.gov.hmcts.darts.authentication.exception.AzureDaoException;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;
import uk.gov.hmcts.darts.authentication.model.Session;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.authentication.service.SessionService;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final SessionService sessionService;
    private final TokenValidator tokenValidator;
    private final AzureDao azureDao;
    private final UriProvider uriProvider;

    @Override
    public URI loginOrRefresh(String sessionId) {
        log.debug("Session {} has initiated login or refresh flow", sessionId);

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            return uriProvider.getAuthorizationUri();
        }

        return uriProvider.getLandingPageUri();
    }

    @Override
    public String handleOauthCode(String sessionId, String code) {
        log.debug("Session {} has presented authorization code {}", sessionId, code);

        OAuthProviderRawResponse tokenResponse;
        try {
            tokenResponse = azureDao.fetchAccessToken(code);
        } catch (AzureDaoException e) {
            throw new AuthenticationException("Failed to obtain access token", e);
        }
        var accessToken = tokenResponse.getAccessToken();

        var validationResult = tokenValidator.validate(accessToken);
        if (!validationResult.valid()) {
            throw new AuthenticationException("Failed to validate access token", validationResult.reason());
        }

        var session = new Session(sessionId, accessToken, tokenResponse.getExpiresIn());
        sessionService.putSession(sessionId, session);

        return accessToken;
    }

}
