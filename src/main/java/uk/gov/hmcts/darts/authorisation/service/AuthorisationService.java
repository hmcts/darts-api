package uk.gov.hmcts.darts.authorisation.service;

import uk.gov.hmcts.darts.authorisation.model.UserState;

public interface AuthorisationService {

    UserState getAuthorisation(String emailAddress);

}
