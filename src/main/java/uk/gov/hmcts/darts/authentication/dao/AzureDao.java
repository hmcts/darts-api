package uk.gov.hmcts.darts.authentication.dao;

import uk.gov.hmcts.darts.authentication.config.AuthConfiguration;
import uk.gov.hmcts.darts.authentication.exception.AzureDaoException;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;

public interface AzureDao {
    OAuthProviderRawResponse fetchAccessToken(String code, AuthConfiguration<?> configuration) throws AzureDaoException;
}
