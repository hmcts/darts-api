package uk.gov.hmcts.darts.authentication.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authentication.client.OAuthClient;
import uk.gov.hmcts.darts.authentication.config.AuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.dao.AzureDao;
import uk.gov.hmcts.darts.authentication.exception.AzureDaoException;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class AzureDaoImpl implements AzureDao {

    private final OAuthClient azureActiveDirectoryClient;

    @Override
    public OAuthProviderRawResponse fetchAccessToken(String code, AuthProviderConfigurationProperties providerConfig,
                                                     AuthConfigurationProperties configuration, String redirectUriOverride)
        throws AzureDaoException {
        log.debug("Fetching access token(s) for authorization code: {}", code);

        if (StringUtils.isBlank(code)) {
            throw new AzureDaoException("Null code not permitted");
        }

        var redirectUri = configuration.getRedirectUri();
        if (redirectUriOverride != null) {
            redirectUri = redirectUriOverride;
        }

        try {
            HTTPResponse response = azureActiveDirectoryClient.fetchAccessToken(providerConfig,
                                                                                redirectUri,
                                                                                code,
                                                                                configuration.getClientId(),
                                                                                configuration.getClientSecret(),
                                                                                configuration.getScope());
            String parsedResponse = response.getContent();

            if (HttpStatus.SC_OK != response.getStatusCode()) {
                throw new AzureDaoException(
                    "Unexpected HTTP response code received from Azure",
                    parsedResponse,
                    response.getStatusCode()
                );
            }

            ObjectMapper mapper = new ObjectMapper();
            OAuthProviderRawResponse tokenResponse = mapper.readValue(
                parsedResponse,
                OAuthProviderRawResponse.class
            );

            return tokenResponse;

        } catch (IOException e) {
            throw new AzureDaoException("Failed to fetch Azure AD Access Token", e);
        }
    }

    @Override
    public OAuthProviderRawResponse fetchAccessToken(String refreshToken, AuthProviderConfigurationProperties providerConfig,
                                                     AuthConfigurationProperties configuration) throws AzureDaoException {

        if (StringUtils.isBlank(refreshToken)) {
            throw new AzureDaoException("Null refresh token not permitted");
        }

        try {
            HTTPResponse response = azureActiveDirectoryClient.fetchAccessToken(providerConfig,
                                                                                refreshToken,
                                                                                configuration.getClientId(),
                                                                                configuration.getClientSecret(),
                                                                                configuration.getScope());
            String parsedResponse = response.getContent();

            if (HttpStatus.SC_OK != response.getStatusCode()) {
                throw new AzureDaoException(
                    "Unexpected HTTP response code received from Azure",
                    parsedResponse,
                    response.getStatusCode()
                );
            }

            ObjectMapper mapper = new ObjectMapper();
            OAuthProviderRawResponse tokenResponse = mapper.readValue(
                parsedResponse,
                OAuthProviderRawResponse.class
            );

            return tokenResponse;

        } catch (IOException e) {
            throw new AzureDaoException("Failed to fetch Azure AD Access Token", e);
        }
    }

}
