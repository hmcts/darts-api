package uk.gov.hmcts.darts.authorisation.api;

import uk.gov.hmcts.darts.authorisation.model.UserState;

public interface AuthorisationApi {

    UserState getAuthorisation(String emailAddress);

}
