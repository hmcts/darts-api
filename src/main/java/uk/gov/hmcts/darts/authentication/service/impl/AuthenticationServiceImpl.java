package uk.gov.hmcts.darts.authentication.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authentication.component.TokenValidator;
import uk.gov.hmcts.darts.authentication.component.UriProvider;
import uk.gov.hmcts.darts.authentication.dao.AzureDao;
import uk.gov.hmcts.darts.authentication.exception.AuthenticationError;
import uk.gov.hmcts.darts.authentication.exception.AzureDaoException;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;
import uk.gov.hmcts.darts.authentication.model.Session;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.authentication.service.SessionService;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

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
            return uriProvider.getLoginUri();
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
            throw new DartsApiException(AuthenticationError.FAILED_TO_OBTAIN_ACCESS_TOKEN, e);
        }
        var accessToken = tokenResponse.getAccessToken();

        var validationResult = tokenValidator.validate(accessToken);
        if (!validationResult.valid()) {
            throw new DartsApiException(AuthenticationError.FAILED_TO_VALIDATE_ACCESS_TOKEN);
        }

        var session = new Session(sessionId, accessToken, tokenResponse.getExpiresIn());
        sessionService.putSession(sessionId, session);

        return accessToken;
    }

    @Override
    public URI logout(String sessionId) {
        log.debug("Session {} has initiated logout flow", sessionId);

        Session session = sessionService.getSession(sessionId);
        if (session == null) {
            throw new DartsApiException(AuthenticationError.LOGOUT_ATTEMPTED_FOR_INACTIVE_SESSION);
        }

        return uriProvider.getLogoutUri(sessionId);
    }

    @Override
    public void invalidateSession(String sessionId) {
        log.debug("Session {} is requesting invalidation", sessionId);

        sessionService.dropSession(sessionId);

        log.debug("Session {} invalidated", sessionId);
    }

    @Override
    public URI resetPassword(String sessionId) {
        log.debug("Session {} is requesting password reset", sessionId);

        return uriProvider.getResetPasswordUri();
    }

}
