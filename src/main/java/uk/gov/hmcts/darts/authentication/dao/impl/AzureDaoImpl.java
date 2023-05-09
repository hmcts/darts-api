package uk.gov.hmcts.darts.authentication.dao.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import uk.gov.hmcts.darts.authentication.config.AuthenticationConfiguration;
import uk.gov.hmcts.darts.authentication.dao.AzureDao;
import uk.gov.hmcts.darts.authentication.exception.AzureDaoException;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;
import uk.gov.hmcts.darts.authentication.service.AzureActiveDirectoryB2CClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@RequiredArgsConstructor
public class AzureDaoImpl implements AzureDao {

    private final AuthenticationConfiguration authConfig;
    private final AzureActiveDirectoryB2CClient azureActiveDirectoryB2CClient;

    @Override
    public OAuthProviderRawResponse fetchAccessToken(String code) throws AzureDaoException {
        log.debug("Fetching access token(s) for authorization code: {}", code);

        if (StringUtils.isBlank(code)) {
            throw new AzureDaoException("Null code not permitted");
        }

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", authConfig.getExternalADauthorizationGrantType());
        requestBody.add("redirect_uri", authConfig.getExternalADredirectUri());
        requestBody.add("code", code);
        requestBody.add("client_id", authConfig.getExternalADclientId());
        requestBody.add("client_secret", authConfig.getExternalADclientSecret());
        requestBody.add("scope", authConfig.getExternalADscope());

        try (Response response = azureActiveDirectoryB2CClient.fetchAccessToken(requestBody)) {
            String parsedResponse = StreamUtils.copyToString(
                response.body().asInputStream(),
                StandardCharsets.UTF_8
            );
            if (HttpStatus.SC_OK != response.status()) {
                throw new AzureDaoException("Unexpected HTTP response code received from Azure",
                                            parsedResponse,
                                            response.status());
            }

            ObjectMapper mapper = new ObjectMapper();
            OAuthProviderRawResponse tokenResponse = mapper.readValue(
                parsedResponse,
                OAuthProviderRawResponse.class
            );

            log.debug("Obtained access tokens for authorization code: {}, {}", code, tokenResponse);
            return tokenResponse;

        } catch (IOException e) {
            throw new AzureDaoException("Failed to fetch Azure AD Access Token", e);
        }
    }

}
