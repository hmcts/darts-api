package uk.gov.hmcts.darts.authentication.component;

import java.net.URI;

public interface UriProvider {

    URI getAuthorizationUri();

    URI getLandingPageUri();

    URI getLogoutPageUri();

}
