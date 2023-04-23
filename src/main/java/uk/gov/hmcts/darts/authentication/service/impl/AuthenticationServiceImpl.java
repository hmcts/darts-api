package uk.gov.hmcts.darts.authentication.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import uk.gov.hmcts.darts.authentication.config.AuthenticationConfiguration;
import uk.gov.hmcts.darts.authentication.model.OAuthProviderRawResponse;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.authentication.service.AzureActiveDirectoryB2CClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    @Autowired
    private AuthenticationConfiguration authServiceConfiguration;

    @Autowired
    private AzureActiveDirectoryB2CClient azureActiveDirectoryB2CClient;

    @Override
    @SneakyThrows
    public String getAuthorizationUrl() {
        URIBuilder uriBuilder = new URIBuilder(
            authServiceConfiguration.getExternalADauthorizationUri());
        uriBuilder.addParameter("client_id", authServiceConfiguration.getExternalADclientId());
        uriBuilder.addParameter("response_type", authServiceConfiguration.getExternalADresponseType());
        uriBuilder.addParameter(
            "redirect_uri",
            authServiceConfiguration.getExternalADredirectUri()
        );
        uriBuilder.addParameter("response_mode", authServiceConfiguration.getExternalADresponseMode());
        uriBuilder.addParameter("scope", authServiceConfiguration.getExternalADscope());
        uriBuilder.addParameter("prompt", authServiceConfiguration.getExternalADprompt());
        return uriBuilder.build().toString();
    }


    @Override
    public OAuthProviderRawResponse fetchAccessToken(String code) {

        OAuthProviderRawResponse rawResponse = new OAuthProviderRawResponse();
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", authServiceConfiguration.getExternalADauthorizationGrantType());
        requestBody.add("redirect_uri", authServiceConfiguration.getExternalADredirectUri());
        requestBody.add("code", code);
        requestBody.add("client_id", authServiceConfiguration.getExternalADclientId());
        requestBody.add("client_secret", authServiceConfiguration.getExternalADclientSecret());
        requestBody.add("resource", authServiceConfiguration.getExternalADclientId());
        requestBody.add("scope", authServiceConfiguration.getExternalADscope());

        try (Response response = azureActiveDirectoryB2CClient.fetchAccessToken(requestBody)) {
            String parsedResponse = StreamUtils.copyToString(
                response.body().asInputStream(),
                StandardCharsets.UTF_8
            );
            if (HttpStatus.SC_OK != response.status()) {
                log.info(
                    "Access Token & Response Status Received from oauth provider **** {} *** Response status : {}",
                    parsedResponse, response.status()
                );
            }

            ObjectMapper mapper = new ObjectMapper();
            rawResponse = mapper.readValue(parsedResponse, OAuthProviderRawResponse.class);

        } catch (IOException e) {
            log.error("Failed to fetch Azure AD Access Token", e);
        }
        return rawResponse;
    }
}
