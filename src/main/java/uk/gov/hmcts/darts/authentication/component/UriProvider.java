package uk.gov.hmcts.darts.authentication.component;

import java.net.URI;

public interface UriProvider {

    URI getLoginUri(String redirectUri);

    URI getLandingPageUri();

    URI getLogoutUri(String accessToken, String redirectUri);

    URI getResetPasswordUri(String redirectUri);

}
