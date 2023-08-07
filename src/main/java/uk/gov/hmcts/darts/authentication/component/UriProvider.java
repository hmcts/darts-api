package uk.gov.hmcts.darts.authentication.component;

import java.net.URI;

public interface UriProvider {

    URI getLoginUri();

    URI getLandingPageUri();

    URI getLogoutUri(String accessToken);

    URI getResetPasswordUri();

}
