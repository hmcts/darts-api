package uk.gov.hmcts.darts.authorisation.api;

import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

import java.util.List;
import java.util.Optional;

public interface AuthorisationApi {

    Optional<UserState> getAuthorisation(String emailAddress);

    void checkAuthorisation(List<CourthouseEntity> courthouses);

}
