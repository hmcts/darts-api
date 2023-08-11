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
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final TokenValidator tokenValidator;
    private final AzureDao azureDao;
    private final UriProvider uriProvider;

    @Override
    public URI loginOrRefresh(String accessToken, String redirectUri) {
        log.debug("Initiated login or refresh flow with access token {}", accessToken);

        if (accessToken == null) {
            return uriProvider.getLoginUri(redirectUri);
        }

        var validationResult = tokenValidator.validate(accessToken);
        if (!validationResult.valid()) {
            return uriProvider.getLoginUri(redirectUri);
        }

        return uriProvider.getLandingPageUri();
    }

    @Override
    public String handleOauthCode(String code) {
        log.debug("Presented authorization code {}", code);

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

        return accessToken;
    }

    @Override
    public URI logout(String accessToken, String redirectUri) {
        log.debug("Initiated logout flow with access token {} and redirectUri {}", accessToken, redirectUri);
        return uriProvider.getLogoutUri(accessToken, redirectUri);
    }

    @Override
    public URI resetPassword(String redirectUri) {
        log.debug("Requesting password reset, with redirectUri {}", redirectUri);
        return uriProvider.getResetPasswordUri(redirectUri);
    }

}
