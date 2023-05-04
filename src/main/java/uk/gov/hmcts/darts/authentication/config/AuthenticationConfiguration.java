package uk.gov.hmcts.darts.authentication.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Getter
public class AuthenticationConfiguration {

    @Value("${spring.security.oauth2.client.registration.external-azure-ad.client-id}")
    private String externalADclientId;

    @Value("${spring.security.oauth2.client.registration.external-azure-ad.client-secret}")
    private String externalADclientSecret;

    @Value("${spring.security.oauth2.client.provider.external-azure-ad-provider.authorization-uri}")
    private String externalADauthorizationUri;

    @Value("${spring.security.oauth2.client.provider.external-azure-ad-provider.token-uri}")
    private String externalADtokenUri;

    @Value("${spring.security.oauth2.client.provider.external-azure-ad-provider.jwk-set-uri}")
    private String externalADjwkSetUri;

    @Value("${spring.security.oauth2.client.registration.external-azure-ad.scope}")
    private String externalADscope;

    @Value("${spring.security.oauth2.client.registration.external-azure-ad.redirect-uri}")
    private String externalADredirectUri;

    @Value("${spring.security.oauth2.client.registration.external-azure-ad.authorization-grant-type}")
    private String externalADauthorizationGrantType;

    @Value("${spring.security.oauth2.client.registration.external-azure-ad.response-type}")
    private String externalADresponseType;

    @Value("${spring.security.oauth2.client.registration.external-azure-ad.response-mode}")
    private String externalADresponseMode;

    @Value("${spring.security.oauth2.client.registration.external-azure-ad.prompt}")
    private String externalADprompt;

    @Value("${spring.security.oauth2.client.registration.external-azure-ad.issuer-uri}")
    private String externalADissuerUri;

}
