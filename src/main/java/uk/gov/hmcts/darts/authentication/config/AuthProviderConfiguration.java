package uk.gov.hmcts.darts.authentication.config;

public interface AuthProviderConfiguration {

    String getAuthorizationURI();

    String getTokenURI();

    String getJKWSURI();

    String getLogoutURI();

    String getResetPasswordURI();
}
