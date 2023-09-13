package uk.gov.hmcts.darts.authentication.config;

public interface AuthConfigurationProperties {

    String getRedirectUri();

    String getLogoutRedirectUri();

    String getIssuerUri();

    String getPrompt();

    String getClientId();

    String getClientSecret();

    String getResponseMode();

    String getScope();

    String getGrantType();

    String getResponseType();

    String getClaims();

}
