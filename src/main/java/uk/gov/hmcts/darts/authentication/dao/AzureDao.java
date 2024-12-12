package uk.gov.hmcts.darts.authentication.dao;

import uk.gov.hmcts.darts.authentication.config.AuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.exception.AzureDaoException;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;

public interface AzureDao {
    OAuthProviderRawResponse fetchAccessToken(String code, AuthProviderConfigurationProperties providerConfig,
                                              AuthConfigurationProperties configuration, String redirectUri) throws AzureDaoException;

    OAuthProviderRawResponse fetchAccessToken(String refreshToken, AuthProviderConfigurationProperties providerConfiguration,
                                              AuthConfigurationProperties configuration) throws AzureDaoException;
}
