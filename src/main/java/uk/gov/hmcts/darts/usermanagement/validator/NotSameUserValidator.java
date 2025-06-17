package uk.gov.hmcts.darts.usermanagement.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.validation.IdRequest;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;

@Component
@RequiredArgsConstructor
public class NotSameUserValidator implements Validator<IdRequest<UserPatch, Integer>> {

    private final AuthorisationApi authorisationApi;

    @Override
    public void validate(IdRequest<UserPatch, Integer> userPatch) {
        if (!(userPatch.getPayload().getActive() != null && !userPatch.getPayload().getActive())) {
            return;
        }
        var currentUser = authorisationApi.getCurrentUser();
        if (currentUser.getId().equals(userPatch.getId())) {
            throw new DartsApiException(AuthorisationError.UNABLE_TO_DEACTIVATE_USER);
        }
    }
}
