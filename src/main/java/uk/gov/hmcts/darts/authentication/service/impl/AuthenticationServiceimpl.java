package uk.gov.hmcts.darts.authentication.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.authentication.component.TokenValidator;
import uk.gov.hmcts.darts.authentication.config.AuthConfiguration;
import uk.gov.hmcts.darts.authentication.config.AuthConfigurationLocator;
import uk.gov.hmcts.darts.authentication.dao.AzureDao;
import uk.gov.hmcts.darts.authentication.exception.AuthenticationError;
import uk.gov.hmcts.darts.authentication.exception.AzureDaoException;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;

@Slf4j
@RequiredArgsConstructor
public class AuthenticationServiceimpl implements AuthenticationService {

    private final TokenValidator tokenValidator;
    private final AzureDao azureDao;

    private final AuthConfigurationLocator locator;

    private final HttpServletRequest request;

    @Override
    public URI loginOrRefresh(String accessToken, String redirectUri) {
        log.debug("Initiated login or refresh flow with access token {}", accessToken);

        if (accessToken == null) {
            return getAuthenticationConfiguration().getLoginUri(redirectUri);
        }

        var validationResult = tokenValidator.validate(accessToken, getAuthenticationConfiguration());
        if (!validationResult.valid()) {
            return getAuthenticationConfiguration().getLoginUri(redirectUri);
        }

        return getAuthenticationConfiguration().getLandingPageUri();
    }

    @Override
    public String handleOauthCode(String code) {
        log.debug("Presented authorization code {}", code);

        OAuthProviderRawResponse tokenResponse;
        try {
            tokenResponse = azureDao.fetchAccessToken(code, getAuthenticationConfiguration());
        } catch (AzureDaoException e) {
            throw new DartsApiException(AuthenticationError.FAILED_TO_OBTAIN_ACCESS_TOKEN, e);
        }
        var accessToken = tokenResponse.getAccessToken();

        var validationResult = tokenValidator.validate(accessToken, getAuthenticationConfiguration());
        if (!validationResult.valid()) {
            throw new DartsApiException(AuthenticationError.FAILED_TO_VALIDATE_ACCESS_TOKEN);
        }

        return accessToken;
    }

    @Override
    public URI logout(String accessToken, String redirectUri) {
        log.debug("Initiated logout flow with access token {} and redirectUri {}", accessToken, redirectUri);
        return getAuthenticationConfiguration().getLogoutUri(accessToken, redirectUri);
    }

    @Override
    public URI resetPassword(String redirectUri) {
        log.debug("Requesting password reset, with redirectUri {}", redirectUri);
        return getAuthenticationConfiguration().getResetPasswordUri(redirectUri);
    }

    private AuthConfiguration<?> getAuthenticationConfiguration()
    {
        return locator.locateAuthenticationConfiguration(locator.getDefaultExternalConfig());
    }
}
