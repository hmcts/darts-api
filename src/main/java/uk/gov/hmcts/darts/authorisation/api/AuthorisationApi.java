package uk.gov.hmcts.darts.authorisation.api;

import uk.gov.hmcts.darts.authorisation.model.UserState;

import java.util.Optional;

public interface AuthorisationApi {

    Optional<UserState> getAuthorisation(String emailAddress);

}
