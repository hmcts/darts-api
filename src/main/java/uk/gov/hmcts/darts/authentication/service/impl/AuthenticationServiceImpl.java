package uk.gov.hmcts.darts.authentication.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authentication.component.TokenValidator;
import uk.gov.hmcts.darts.authentication.config.AuthStrategySelector;
import uk.gov.hmcts.darts.authentication.config.AuthenticationConfigurationPropertiesStrategy;
import uk.gov.hmcts.darts.authentication.dao.AzureDao;
import uk.gov.hmcts.darts.authentication.exception.AuthenticationError;
import uk.gov.hmcts.darts.authentication.exception.AzureDaoException;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;
import uk.gov.hmcts.darts.authentication.model.TokenResponse;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.net.URI;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final TokenValidator tokenValidator;
    private final AzureDao azureDao;
    private final AuthStrategySelector locator;

    @Override
    public URI loginOrRefresh(String accessToken, String redirectUri) {
        log.debug("Initiated login or refresh flow with access token {}", accessToken);

        AuthenticationConfigurationPropertiesStrategy configStrategy = locator.locateAuthenticationConfiguration();
        if (accessToken == null) {
            return configStrategy.getLoginUri(redirectUri);
        }

        var validationResult = tokenValidator.validate(accessToken, configStrategy.getProviderConfiguration(), configStrategy.getConfiguration());

        if (!validationResult.valid()) {
            return configStrategy.getLoginUri(redirectUri);
        }

        return configStrategy.getLandingPageUri();
    }

    @Override
    public TokenResponse handleOauthCode(String code, String redirectUri) {
        AuthenticationConfigurationPropertiesStrategy configStrategy = locator.locateAuthenticationConfiguration();

        log.debug("Presented authorization code {}", code);

        OAuthProviderRawResponse tokenResponse;
        try {
            tokenResponse = azureDao.fetchAccessToken(code, configStrategy.getProviderConfiguration(), configStrategy.getConfiguration(), redirectUri);
        } catch (AzureDaoException e) {
            throw new DartsApiException(AuthenticationError.FAILED_TO_OBTAIN_ACCESS_TOKEN, e);
        }
        var refreshToken = tokenResponse.getRefreshToken();
        var accessToken = Objects.nonNull(tokenResponse.getIdToken()) ? tokenResponse.getIdToken() : tokenResponse.getAccessToken();

        var validationResult = tokenValidator.validate(accessToken, configStrategy.getProviderConfiguration(), configStrategy.getConfiguration());
        if (!validationResult.valid()) {
            throw new DartsApiException(AuthenticationError.FAILED_TO_VALIDATE_ACCESS_TOKEN);
        }

        return new TokenResponse(accessToken, refreshToken);
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        AuthenticationConfigurationPropertiesStrategy configStrategy = locator.locateAuthenticationConfiguration();

        OAuthProviderRawResponse tokenResponse;
        try {
            tokenResponse = azureDao.fetchAccessToken(refreshToken, configStrategy.getProviderConfiguration(), configStrategy.getConfiguration());
        } catch (AzureDaoException e) {
            throw new DartsApiException(AuthenticationError.FAILED_TO_OBTAIN_ACCESS_TOKEN, e);
        }
        var accessToken = Objects.nonNull(tokenResponse.getIdToken()) ? tokenResponse.getIdToken() : tokenResponse.getAccessToken();

        var validationResult = tokenValidator.validate(accessToken, configStrategy.getProviderConfiguration(), configStrategy.getConfiguration());
        if (!validationResult.valid()) {
            throw new DartsApiException(AuthenticationError.FAILED_TO_VALIDATE_ACCESS_TOKEN);
        }

        return accessToken;
    }

    @Override
    public URI logout(String accessToken, String redirectUri) {
        AuthenticationConfigurationPropertiesStrategy configStrategy = locator.locateAuthenticationConfiguration();

        log.debug("Initiated logout flow with access token {} and redirectUri {}", accessToken, redirectUri);
        return configStrategy.getLogoutUri(accessToken, redirectUri);
    }

    @Override
    public URI resetPassword(String redirectUri) {
        AuthenticationConfigurationPropertiesStrategy configStrategy = locator.locateAuthenticationConfiguration();

        log.debug("Requesting password reset, with redirectUri {}", redirectUri);
        return configStrategy.getResetPasswordUri(redirectUri);
    }
}
