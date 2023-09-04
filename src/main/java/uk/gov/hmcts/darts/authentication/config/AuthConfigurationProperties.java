package uk.gov.hmcts.darts.authentication.config;

public interface AuthConfigurationProperties {

    String getRedirectURI();

    String getLogoutRedirectURI();

    String getIssuerURI();

    String getPrompt();

    String getClientId();

    String getClientSecret();

    String getResponseMode();

    String getScope();

    String getGrantType();

    String getResponseType();

}
