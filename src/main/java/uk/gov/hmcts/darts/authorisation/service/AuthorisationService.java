package uk.gov.hmcts.darts.authorisation.service;

import uk.gov.hmcts.darts.authorisation.model.UserState;

import java.util.Optional;

public interface AuthorisationService {

    Optional<UserState> getAuthorisation(String emailAddress);

}
